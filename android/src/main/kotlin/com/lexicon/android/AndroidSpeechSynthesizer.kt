package com.lexicon.android

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AndroidSpeechSynthesizer(
    private val context: Context,
    private val settings: VoicePreference,
) : SpeechSynthesizer {
    private var engine: TextToSpeech? = null

    private suspend fun engine(): TextToSpeech =
        engine ?: suspendCancellableCoroutine { continuation ->
            lateinit var tts: TextToSpeech
            tts =
                TextToSpeech(context) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        engine = tts
                        continuation.resume(tts)
                    } else {
                        val message = "TextToSpeech engine failed to initialize (status=$status)"
                        continuation.resumeWithException(SpeechSynthesisFailed(message))
                    }
                }
        }

    /**
     * One entry per voice that actually sounds different.
     *
     * Android lists the same speaker several times over — `pl-pl-x-oda-local` and
     * `pl-pl-x-oda-network` are one voice fetched two ways — so a raw listing is mostly
     * duplicates. Entries are grouped by the speaker code in the middle of the name and
     * only the best copy of each is kept: the one that needs no network, then the one
     * with the higher quality, so the choice is offline and the list is short.
     */
    override suspend fun voices(): List<SpeechVoice> {
        val tts = runCatching { engine() }.getOrNull() ?: return emptyList()
        return tts.voices
            .orEmpty()
            .filter { it.locale.language == POLISH.language }
            .filterNot { TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED in it.features }
            .groupBy { it.speaker() }
            .toSortedMap()
            .values
            .mapNotNull { copies -> copies.minWithOrNull(BEST_COPY) }
            .map { SpeechVoice(id = it.name) }
    }

    override suspend fun speak(
        text: String,
        locale: Locale,
    ) {
        val tts = engine()
        tts.language = locale
        settings.preferredVoiceId()?.let { preferred ->
            tts.voices.orEmpty().firstOrNull { it.name == preferred }?.let { tts.voice = it }
        }
        val utteranceId = UUID.randomUUID().toString()
        suspendCancellableCoroutine<Unit> { continuation ->
            tts.setOnUtteranceProgressListener(
                object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit

                    override fun onDone(utteranceId: String?) {
                        if (continuation.isActive) continuation.resume(Unit)
                    }

                    @Deprecated("Deprecated in TextToSpeech", ReplaceWith(""))
                    override fun onError(utteranceId: String?) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(SpeechSynthesisFailed("Playback failed for utterance $utteranceId"))
                        }
                    }
                },
            )
            val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            if (result == TextToSpeech.ERROR && continuation.isActive) {
                continuation.resumeWithException(SpeechSynthesisFailed("speak() returned ERROR for \"$text\""))
            }
        }
    }
}

/** Prefer a voice that works offline, then the better-sounding one. */
private val BEST_COPY = compareBy<Voice>({ if (it.isNetworkConnectionRequired) 1 else 0 }, { -it.quality })

private val POLISH = Locale.forLanguageTag("pl-PL")

/**
 * The part of a voice name that identifies who is speaking.
 *
 * Names run `pl-pl-x-oda-local`; everything after the speaker code says how the voice is
 * delivered rather than what it sounds like.
 */
private fun Voice.speaker(): String = name.removeSuffix("-local").removeSuffix("-network")

class SpeechSynthesisFailed(message: String) : Exception(message)
