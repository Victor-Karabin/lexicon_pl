package com.lexicon.boundary

import com.lexicon.model.vocabulary.Aspect

data class VerbConjugationBoundary(
    val infinitive: String,
    val forms: Map<String, List<String>>,
    val translation: String? = null,
    val example: String = "",
    val aspect: Aspect? = null,
)

data class ConjugationCourseBoundary(
    val id: String,
    val infinitives: List<String>,
)

data class ConjugationProgressBoundary(
    val infinitive: String,
    val person: String,
    val attempted: Int,
    val correct: Int,
    val incorrect: Int,
    val streak: Int,
)

interface ConjugationRepository {
    suspend fun seedFromAsset(): SeedOutcomeBoundary

    suspend fun countVerbs(): Int

    suspend fun verbs(): List<VerbConjugationBoundary>

    suspend fun verbPage(
        query: String,
        limit: Int,
        offset: Int,
    ): List<VerbConjugationBoundary>

    suspend fun verb(infinitive: String): VerbConjugationBoundary?

    suspend fun saveUserVerb(verb: VerbConjugationBoundary)

    suspend fun deleteUserVerb(infinitive: String)

    suspend fun deleteVerb(infinitive: String)

    suspend fun hasDeletedVerbs(): Boolean

    suspend fun restoreVerbs()

    suspend fun courses(): List<ConjugationCourseBoundary>

    suspend fun createCourse(infinitives: List<String>): String

    suspend fun deleteCourse(courseId: String)

    suspend fun progress(courseId: String): List<ConjugationProgressBoundary>

    suspend fun recordAttempt(
        courseId: String,
        infinitive: String,
        person: String,
        isCorrect: Boolean,
    )
}
