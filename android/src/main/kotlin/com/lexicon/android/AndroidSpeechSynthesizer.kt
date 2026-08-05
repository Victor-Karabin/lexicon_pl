package com.lexicon.android

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class AndroidSpeechSynthesizer
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
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

        override suspend fun speak(
            text: String,
            locale: Locale,
        ) {
            val tts = engine()
            tts.language = locale
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
