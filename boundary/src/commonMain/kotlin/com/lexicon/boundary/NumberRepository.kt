package com.lexicon.boundary

data class NumberProgressBoundary(
    val value: Long,
    val kind: String,
    val attempted: Int,
    val correct: Int,
    val incorrect: Int,
    val streak: Int,
)

interface NumberRepository {
    suspend fun spec(): String?

    suspend fun saveSpec(spec: String)

    suspend fun progress(): List<NumberProgressBoundary>

    suspend fun recordAttempt(
        value: Long,
        kind: String,
        isCorrect: Boolean,
    )
}
