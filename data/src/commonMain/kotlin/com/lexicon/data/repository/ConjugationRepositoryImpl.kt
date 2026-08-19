package com.lexicon.data.repository

import com.lexicon.boundary.ConjugationProgressBoundary
import com.lexicon.boundary.ConjugationRepository
import com.lexicon.boundary.VerbConjugationBoundary
import com.lexicon.data.local.ConjugationAssetLoader
import com.lexicon.data.local.ConjugationDao
import com.lexicon.data.local.ConjugationProgressEntity
import com.lexicon.data.local.ConjugationSelectionEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ConjugationRepositoryImpl(
    private val loader: ConjugationAssetLoader,
    private val dao: ConjugationDao,
) : ConjugationRepository {
    private val lock = Mutex()
    private var cached: List<VerbConjugationBoundary>? = null

    override suspend fun verbs(): List<VerbConjugationBoundary> =
        lock.withLock {
            cached ?: loader.load().also { cached = it }
        }

    override suspend fun selectedInfinitives(): List<String> = dao.selection()

    override suspend fun selectInfinitives(infinitives: List<String>) {
        dao.clearSelection()
        dao.addToSelection(infinitives.distinct().map(::ConjugationSelectionEntity))
    }

    override suspend fun progress(): List<ConjugationProgressBoundary> =
        dao.progress().map {
            ConjugationProgressBoundary(
                infinitive = it.infinitive,
                person = it.person,
                attempted = it.attempted,
                correct = it.correct,
                incorrect = it.incorrect,
                streak = it.streak,
            )
        }

    override suspend fun recordAttempt(
        infinitive: String,
        person: String,
        isCorrect: Boolean,
    ) {
        val existing = dao.progressFor(infinitive, person)
        dao.save(
            ConjugationProgressEntity(
                infinitive = infinitive,
                person = person,
                attempted = (existing?.attempted ?: 0) + 1,
                correct = (existing?.correct ?: 0) + if (isCorrect) 1 else 0,
                incorrect = (existing?.incorrect ?: 0) + if (isCorrect) 0 else 1,
                streak = if (isCorrect) (existing?.streak ?: 0) + 1 else 0,
            ),
        )
    }

    override suspend fun resetProgress() = dao.clearProgress()
}
