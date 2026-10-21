package com.lexicon.boundary

data class CountryBoundary(
    val code: String,
    val english: String,
    val polish: List<String>,
    val man: String?,
    val woman: String?,
    val capital: String,
    val capitalEnglish: String,
)

data class CountryProgressBoundary(
    val code: String,
    val field: String,
    val attempted: Int,
    val correct: Int,
    val incorrect: Int,
    val streak: Int,
)

interface CountryRepository {
    suspend fun countries(): List<CountryBoundary>

    suspend fun chosen(): List<String>?

    suspend fun saveChosen(codes: List<String>)

    suspend fun progress(): List<CountryProgressBoundary>

    suspend fun recordAttempt(
        code: String,
        field: String,
        isCorrect: Boolean,
    )
}
