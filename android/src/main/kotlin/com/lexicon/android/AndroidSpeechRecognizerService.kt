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
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

                    recognizer.setRecognitionListener(
                        object : RecognitionListener {
                            override fun onResults(results: Bundle?) {
                                val text =
                                    results
                                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                        ?.firstOrNull()
                                        .orEmpty()
                                val confidence = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)?.firstOrNull()
                                recognizer.destroy()
                                if (continuation.isActive) continuation.resume(SpeechRecognitionResult(text, confidence))
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

                            override fun onBufferReceived(buffer: ByteArray?) = Unit

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
    }
