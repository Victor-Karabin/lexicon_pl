package com.lexicon.boundary

data class VerbConjugationBoundary(
    val infinitive: String,
    val forms: Map<String, List<String>>,
    val translation: String? = null,
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
    suspend fun syncFromSource(): SyncOutcomeBoundary

    suspend fun countVerbs(): Int

    suspend fun verbs(): List<VerbConjugationBoundary>

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
