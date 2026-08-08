package com.lexicon.android

import java.util.Locale

data class SpeechRecognitionResult(
    val recognizedText: String,
    val confidence: Float?,
    val audioFilePath: String?,
)

interface SpeechRecognizerService {
    suspend fun recognize(locale: Locale = Locale.forLanguageTag("pl-PL")): SpeechRecognitionResult
}

class SpeechRecognitionFailed(message: String) : Exception(message)
