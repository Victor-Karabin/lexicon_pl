package com.lexicon.boundary

data class SpeechRecognitionResult(
    val recognizedText: String,
    val confidence: Float?,
    val audioFilePath: String?,
)

interface SpeechRecognizerService {
    suspend fun recognize(): SpeechRecognitionResult
}

enum class SpeechRecognitionFailureReason { UNAVAILABLE, FAILED }

class SpeechRecognitionFailed(
    message: String,
    val reason: SpeechRecognitionFailureReason,
) : Exception(message)
