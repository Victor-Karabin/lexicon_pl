package com.lexicon.boundary

data class SentenceRequestBoundary(
    val word: String,
    val translation: String,
    val level: String,
    val context: String,
    val requiredWords: List<String>,
)

sealed interface SentenceResultBoundary {
    data class Generated(val sentence: String) : SentenceResultBoundary

    data object Offline : SentenceResultBoundary

    data class Refused(val reason: String) : SentenceResultBoundary
}

interface SentenceGenerator {
    suspend fun generate(request: SentenceRequestBoundary): SentenceResultBoundary
}
