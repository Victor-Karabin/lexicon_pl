package com.lexicon.android.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.boundary.SpeechVoice
import com.lexicon.boundary.VoiceGender
import com.lexicon.boundary.chosen
import com.lexicon.common.runSuspendCatching
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AndroidSpeechSynthesizer(
    private val context: Context,
    private val settings: VoicePreference,
) : SpeechSynthesizer {
    private val engineLock = Mutex()

    @Volatile private var engine: TextToSpeech? = null
    private val pending = ConcurrentHashMap<String, CancellableContinuation<Unit>>()

    private suspend fun engine(): TextToSpeech = engine ?: engineLock.withLock { engine ?: createEngine().also { engine = it } }

    private suspend fun createEngine(): TextToSpeech =
        suspendCancellableCoroutine { continuation ->
            lateinit var tts: TextToSpeech
            tts =
                TextToSpeech(context) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        tts.setOnUtteranceProgressListener(listener)
                        if (continuation.isActive) continuation.resume(tts) else tts.shutdown()
                    } else {
                        tts.shutdown()
                        val message = "TextToSpeech engine failed to initialize (status=$status)"
                        if (continuation.isActive) continuation.resumeWithException(SpeechSynthesisFailed(message))
                    }
                }
        }

    private val listener =
        object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) {
                finish(utteranceId)
            }

            override fun onStop(
                utteranceId: String?,
                interrupted: Boolean,
            ) {
                finish(utteranceId)
            }

            @Deprecated("Deprecated in TextToSpeech", ReplaceWith(""))
            override fun onError(utteranceId: String?) {
                finish(utteranceId, SpeechSynthesisFailed("Playback failed for utterance $utteranceId"))
            }
        }

    private fun finish(
        utteranceId: String?,
        failure: Throwable? = null,
    ) {
        val continuation = utteranceId?.let { pending.remove(it) } ?: return
        if (!continuation.isActive) return
        if (failure == null) continuation.resume(Unit) else continuation.resumeWithException(failure)
    }

    override suspend fun voices(): List<SpeechVoice> {
        val tts = runSuspendCatching { engine() }.getOrElse { failure ->
            Log.w(TAG, "The device voice is unavailable", failure)
            return emptyList()
        }
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
            pending[utteranceId] = continuation
            continuation.invokeOnCancellation {
                if (pending.remove(utteranceId) != null) tts.stop()
            }
            if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId) == TextToSpeech.ERROR) {
                finish(utteranceId, SpeechSynthesisFailed("speak() returned ERROR"))
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

private const val TAG = "AndroidSpeechSynthesizer"
