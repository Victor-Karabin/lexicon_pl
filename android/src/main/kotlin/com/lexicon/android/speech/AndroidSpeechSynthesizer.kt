package com.lexicon.android.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.boundary.SpeechVoice
import com.lexicon.boundary.VoiceGender
import com.lexicon.boundary.chosen
import kotlinx.coroutines.suspendCancellableCoroutine
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
                    gender = VoiceGender.NEUTRAL,
                )
            }
    }

    override suspend fun speak(text: String) {
        val tts = engine()
        tts.language = POLISH

        voices().chosen(settings.preferredVoiceId())?.let { chosen ->
            tts.voices.orEmpty().firstOrNull { it.name == chosen.id }?.let { tts.voice = it }
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

private fun List<Voice>.dropPoorOnes(): List<Voice> = filter { it.quality >= Voice.QUALITY_NORMAL }.ifEmpty { this }

private val BEST_COPY = compareBy<Voice>({ if (it.isNetworkConnectionRequired) 1 else 0 }, { -it.quality })

private fun Voice.speaker(): String {
    val lower = name.lowercase()
    return SPEAKER_CODE.find(lower)?.groupValues?.get(1)
        ?: lower.removeSuffix("-local").removeSuffix("-network")
}

private val SPEAKER_CODE = Regex("-x-([a-z0-9]+?)(?:-|#|$)")

private fun Voice.isAlias(): Boolean = name.lowercase().endsWith("-language")

private val VOICE_NAMES = listOf(
    "Zofia", "Marek", "Hanna", "Piotr", "Alicja", "Tomasz",
    "Maja", "Jakub", "Nina", "Rafał", "Ewa", "Kamil",
)

class SpeechSynthesisFailed(message: String) : Exception(message)
