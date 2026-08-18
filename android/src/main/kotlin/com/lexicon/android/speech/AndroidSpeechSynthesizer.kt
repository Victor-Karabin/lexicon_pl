package com.lexicon.android.speech

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
            .filterNot { it.isAlias() }
            .groupBy { it.speaker() }
            .toSortedMap()
            .values
            .mapNotNull { copies -> copies.minWithOrNull(BEST_COPY) }
            .dropPoorOnes()
            .mapIndexed { index, voice ->
                SpeechVoice(
                    id = voice.name,
                    displayName = VOICE_NAMES[index % VOICE_NAMES.size],
                    // The platform will not say, and the id does not encode it.
                    gender = VoiceGender.NEUTRAL,
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

/**
 * Drops the voices the engine itself rates below ordinary quality.
 *
 * Devices carry a few thin, buzzy voices nobody would choose to learn from. They are only
 * dropped while something better remains — a short list beats an empty one, and this list
 * is the fallback for when Cloud cannot be reached at all.
 */
private fun List<Voice>.dropPoorOnes(): List<Voice> = filter { it.quality >= Voice.QUALITY_NORMAL }.ifEmpty { this }

/** Prefer a voice that works offline, then the better-sounding one. */
private val BEST_COPY = compareBy<Voice>({ if (it.isNetworkConnectionRequired) 1 else 0 }, { -it.quality })

private val POLISH = Locale.forLanguageTag("pl-PL")

/**
 * The part of a voice name that identifies who is speaking.
 *
 * Names usually run `pl-pl-x-oda-local`, where `oda` is the speaker and the rest says how
 * the voice is delivered and at what quality. Engines are inconsistent about the rest of
 * it — case differs, and the same speaker turns up with several suffixes — so the speaker
 * code alone is the key where there is one.
 */
private fun Voice.speaker(): String {
    val lower = name.lowercase()
    return SPEAKER_CODE.find(lower)?.groupValues?.get(1)
        ?: lower.removeSuffix("-local").removeSuffix("-network")
}

private val SPEAKER_CODE = Regex("-x-([a-z0-9]+?)(?:-|#|$)")

/**
 * Whether a voice is the locale's stand-in rather than a voice of its own.
 *
 * Engines list `pl-pl-language` alongside the real voices as a pointer to whichever one
 * is the default. It carries its own name, so it survives grouping by speaker, and it
 * sounds exactly like the voice it points at — one way the same voice appears twice.
 */
private fun Voice.isAlias(): Boolean = name.lowercase().endsWith("-language")

/**
 * Polish names for the device's voices.
 *
 * Assigned by position in the deduplicated list, so a voice keeps its name between
 * launches. Android's Voice carries no gender and its ids do not encode one, so which
 * name lands on which voice is arbitrary — the names are labels to pick by after
 * listening, not a claim about who is speaking.
 */
private val VOICE_NAMES = listOf(
    "Zofia", "Marek", "Hanna", "Piotr", "Alicja", "Tomasz",
    "Maja", "Jakub", "Nina", "Rafał", "Ewa", "Kamil",
)

class SpeechSynthesisFailed(message: String) : Exception(message)
