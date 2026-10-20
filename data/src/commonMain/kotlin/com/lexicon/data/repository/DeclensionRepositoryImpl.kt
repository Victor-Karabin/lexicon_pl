package com.lexicon.data.repository

import com.lexicon.boundary.DeclensionProgressBoundary
import com.lexicon.boundary.DeclensionRepository
import com.lexicon.data.local.DeclensionDao
import com.lexicon.data.local.DeclensionProgressEntity
import com.lexicon.data.local.VocabularySeeder
import com.lexicon.data.local.toWord
import com.lexicon.model.vocabulary.Word

class DeclensionRepositoryImpl(
    private val dao: DeclensionDao,
    private val vocabularySeeder: VocabularySeeder,
) : DeclensionRepository {
    override suspend fun declinableWords(
        foldedQuery: String,
        limit: Int,
        offset: Int,
    ): List<Word> {
        vocabularySeeder.ensureSeeded()
        return dao.declinableWords(foldedQuery, limit, offset).map { it.toWord() }
    }

    override suspend fun studySetWords(): List<Word> {
        vocabularySeeder.ensureSeeded()
        return dao.studySetWords().map { it.toWord() }
    }

    override suspend fun progress(courseId: String): List<DeclensionProgressBoundary> =
        dao.progress(courseId).map {
            DeclensionProgressBoundary(
                word = it.word,
                column = it.column,
                case = it.grammaticalCase,
                attempted = it.attempted,
                correct = it.correct,
                incorrect = it.incorrect,
                streak = it.streak,
            )
        }

    override suspend fun recordAttempt(
        courseId: String,
        word: String,
        column: String,
        case: String,
        isCorrect: Boolean,
    ) {
        val existing = dao.progressFor(courseId, word, column, case)
        dao.save(
            DeclensionProgressEntity(
                courseId = courseId,
                word = word,
                column = column,
                grammaticalCase = case,
                attempted = (existing?.attempted ?: 0) + 1,
                correct = (existing?.correct ?: 0) + if (isCorrect) 1 else 0,
                incorrect = (existing?.incorrect ?: 0) + if (isCorrect) 0 else 1,
                streak = if (isCorrect) (existing?.streak ?: 0) + 1 else 0,
            ),
        )
    }
}
