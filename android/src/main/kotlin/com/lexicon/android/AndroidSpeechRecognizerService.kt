package com.lexicon.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.lexicon.common.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Sample rate the platform's [SpeechRecognizer] streams raw audio at via [RecognitionListener.onBufferReceived]. */
private const val AUDIO_SAMPLE_RATE_HZ = 16_000
private const val BITS_PER_SAMPLE = 16
private const val WAV_HEADER_SIZE = 44

@Singleton
class AndroidSpeechRecognizerService
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val dispatchers: DispatcherProvider,
    ) : SpeechRecognizerService {
        override suspend fun recognize(locale: Locale): SpeechRecognitionResult {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                throw SpeechRecognitionFailed("Speech recognition is not available on this device")
            }
            // SpeechRecognizer must be created and driven from the main thread.
            return withContext(dispatchers.main) {
                suspendCancellableCoroutine { continuation ->
                    val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                    val intent =
                        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
                        }
                    // Raw PCM streamed in parallel with recognition, kept only so the user can play back
                    // what they said — not guaranteed on every device/OS version.
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
                                recognizer.destroy()
                                if (continuation.isActive) {
                                    continuation.resumeWithException(SpeechRecognitionFailed("Recognition error code $error"))
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

        /** Wraps the raw PCM stream in a canonical WAV header and caches it. Returns null for an empty capture. */
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
            buffer.putShort(1) // PCM
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
