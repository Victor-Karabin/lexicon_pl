package com.lexicon.android.recognition

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.lexicon.android.speech.POLISH_LANGUAGE_TAG
import com.lexicon.boundary.SpeechRecognitionFailed
import com.lexicon.boundary.SpeechRecognitionResult
import com.lexicon.boundary.SpeechRecognizerService
import com.lexicon.common.DispatcherProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val AUDIO_SAMPLE_RATE_HZ = 16_000
private const val BITS_PER_SAMPLE = 16
private const val WAV_HEADER_SIZE = 44
private const val TAG = "AndroidSpeechRecognizer"

private fun speechRecognizerErrorName(error: Int): String =
    when (error) {
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ERROR_NETWORK_TIMEOUT"
        SpeechRecognizer.ERROR_NETWORK -> "ERROR_NETWORK"
        SpeechRecognizer.ERROR_AUDIO -> "ERROR_AUDIO"
        SpeechRecognizer.ERROR_SERVER -> "ERROR_SERVER"
        SpeechRecognizer.ERROR_CLIENT -> "ERROR_CLIENT"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "ERROR_SPEECH_TIMEOUT"
        SpeechRecognizer.ERROR_NO_MATCH -> "ERROR_NO_MATCH"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ERROR_RECOGNIZER_BUSY"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "ERROR_INSUFFICIENT_PERMISSIONS"
        10 -> "ERROR_TOO_MANY_REQUESTS"
        11 -> "ERROR_SERVER_DISCONNECTED"
        12 -> "ERROR_LANGUAGE_NOT_SUPPORTED"
        13 -> "ERROR_LANGUAGE_UNAVAILABLE"
        else -> "UNKNOWN"
    }

class AndroidSpeechRecognizerService(
    private val context: Context,
    private val dispatchers: DispatcherProvider,
) : SpeechRecognizerService {
    override suspend fun recognize(): SpeechRecognitionResult {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "isRecognitionAvailable() is false — no speech recognizer service on this device/emulator image")
            throw SpeechRecognitionFailed("Speech recognition is not available on this device")
        }
        return withContext(dispatchers.main) {
            suspendCancellableCoroutine { continuation ->
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                val intent =
                    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, POLISH_LANGUAGE_TAG)
                    }
                val recordedAudio = ByteArrayOutputStream()

                recognizer.setRecognitionListener(
                    object : RecognitionListener {
                        override fun onResults(results: Bundle?) {
                            val text =
                                results
                                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                    ?.firstOrNull()
                                    .orEmpty()
                            val confidence = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)?.firstOrNull()
                            val audioFilePath = runCatching { writeWavFile(recordedAudio.toByteArray()) }.getOrNull()
                            recognizer.destroy()
                            if (continuation.isActive) {
                                continuation.resume(SpeechRecognitionResult(text, confidence, audioFilePath))
                            }
                        }

                        override fun onError(error: Int) {
                            val errorName = speechRecognizerErrorName(error)
                            Log.w(TAG, "SpeechRecognizer error: $error ($errorName) for $POLISH_LANGUAGE_TAG")
                            recognizer.destroy()
                            if (continuation.isActive) {
                                continuation.resumeWithException(SpeechRecognitionFailed("Recognition error $error ($errorName)"))
                            }
                        }

                        override fun onReadyForSpeech(params: Bundle?) = Unit

                        override fun onBeginningOfSpeech() = Unit

                        override fun onRmsChanged(rmsdB: Float) = Unit

                        override fun onBufferReceived(buffer: ByteArray?) {
                            buffer?.let { recordedAudio.write(it) }
                        }

                        override fun onEndOfSpeech() = Unit

                        override fun onPartialResults(partialResults: Bundle?) = Unit

                        override fun onEvent(
                            eventType: Int,
                            params: Bundle?,
                        ) = Unit
                    },
                )

                continuation.invokeOnCancellation { recognizer.destroy() }
                recognizer.startListening(intent)
            }
        }
    }

    private fun writeWavFile(pcmData: ByteArray): String? {
        if (pcmData.isEmpty()) return null
        val file = File(context.cacheDir, "pronunciation_attempt_${System.currentTimeMillis()}.wav")
        file.outputStream().use { out ->
            writeWavHeader(out, pcmData.size)
            out.write(pcmData)
        }
        return file.absolutePath
    }

    private fun writeWavHeader(
        out: OutputStream,
        pcmDataSize: Int,
    ) {
        val channels = 1
        val byteRate = AUDIO_SAMPLE_RATE_HZ * channels * BITS_PER_SAMPLE / 8
        val blockAlign = channels * BITS_PER_SAMPLE / 8
        val buffer = ByteBuffer.allocate(WAV_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray())
        buffer.putInt(36 + pcmDataSize)
        buffer.put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16)
        buffer.putShort(1)
        buffer.putShort(channels.toShort())
        buffer.putInt(AUDIO_SAMPLE_RATE_HZ)
        buffer.putInt(byteRate)
        buffer.putShort(blockAlign.toShort())
        buffer.putShort(BITS_PER_SAMPLE.toShort())
        buffer.put("data".toByteArray())
        buffer.putInt(pcmDataSize)
        out.write(buffer.array())
    }
}
