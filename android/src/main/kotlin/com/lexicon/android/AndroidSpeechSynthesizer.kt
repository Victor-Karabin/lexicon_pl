package com.lexicon.android

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Polish given names for the device's voices.
 *
 * Android names a voice "pl-pl-x-oda-local", which is no use in a settings list, and
 * exposes nothing about who it sounds like — Voice carries no gender. So a name is
 * assigned from this list by the voice's own id, which keeps it stable between
 * launches, and the learner picks by listening rather than by reading.
 */
private val VOICE_NAMES = listOf(
    "Zofia", "Marek", "Hanna", "Piotr", "Alicja", "Tomasz",
    "Maja", "Jakub", "Nina", "Rafał", "Ewa", "Kamil",
)

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

    override suspend fun voices(): List<SpeechVoice> {
        val tts = runCatching { engine() }.getOrNull() ?: return emptyList()
        return tts.voices
            .orEmpty()
            .filter { it.locale.language == Locale.forLanguageTag("pl-PL").language }
            .sortedBy { it.name }
            .mapIndexed { index, voice ->
                SpeechVoice(
                    id = voice.name,
                    displayName = VOICE_NAMES[(voice.name.hashCode().mod(VOICE_NAMES.size) + index).mod(VOICE_NAMES.size)],
                )
            }
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

class SpeechSynthesisFailed(message: String) : Exception(message)
