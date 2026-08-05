package com.lexicon.android

import java.util.Locale

data class SpeechRecognitionResult(
    val recognizedText: String,
    /** 0f-1f confidence, when the platform reports one. */
    val confidence: Float?,
)

/** Records and transcribes a single utterance. Backed by the on-device Speech Recognizer. */
interface SpeechRecognizerService {
    suspend fun recognize(locale: Locale = Locale.forLanguageTag("pl-PL")): SpeechRecognitionResult
}

class SpeechRecognitionFailed(message: String) : Exception(message)
