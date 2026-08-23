package com.lexicon.boundary

data class ExampleRequestBoundary(
    val word: String,
    val translation: String,
    val level: String,
)

interface ExampleSentenceGenerator {
    suspend fun generate(request: ExampleRequestBoundary): SentenceResultBoundary
}
