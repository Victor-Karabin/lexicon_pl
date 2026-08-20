package com.lexicon.boundary

data class SpeechRecognitionResult(
    val recognizedText: String,
    val confidence: Float?,
    val audioFilePath: String?,
)

interface SpeechRecognizerService {
    suspend fun recognize(): SpeechRecognitionResult
}

class SpeechRecognitionFailed(message: String) : Exception(message)
