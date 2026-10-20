package com.lexicon.boundary

import com.lexicon.model.vocabulary.Word

data class DeclensionProgressBoundary(
    val word: String,
    val column: String,
    val case: String,
    val attempted: Int,
    val correct: Int,
    val incorrect: Int,
    val streak: Int,
)

interface DeclensionRepository {
    suspend fun declinableWords(
        foldedQuery: String,
        limit: Int,
        offset: Int,
    ): List<Word>

    suspend fun studySetWords(): List<Word>

    suspend fun progress(courseId: String): List<DeclensionProgressBoundary>

    suspend fun recordAttempt(
        courseId: String,
        word: String,
        column: String,
        case: String,
        isCorrect: Boolean,
    )
}
