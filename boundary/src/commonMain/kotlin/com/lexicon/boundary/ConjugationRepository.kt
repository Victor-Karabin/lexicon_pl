package com.lexicon.boundary

data class VerbConjugationBoundary(
    val infinitive: String,
    val forms: Map<String, List<String>>,
    val translation: String? = null,
)

interface ConjugationRepository {
    suspend fun verbs(): List<VerbConjugationBoundary>

    suspend fun selectedInfinitives(): List<String>

    suspend fun selectInfinitives(infinitives: List<String>)

    suspend fun progress(): List<ConjugationProgressBoundary>

    suspend fun recordAttempt(
        infinitive: String,
        person: String,
        isCorrect: Boolean,
    )

    suspend fun resetProgress()
}

data class ConjugationProgressBoundary(
    val infinitive: String,
    val person: String,
    val attempted: Int,
    val correct: Int,
    val incorrect: Int,
    val streak: Int,
)
