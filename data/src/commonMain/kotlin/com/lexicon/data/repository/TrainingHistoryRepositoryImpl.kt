package com.lexicon.data.repository

import com.lexicon.boundary.AccuracyBoundary
import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.data.local.TrainingResultDao
import com.lexicon.data.local.TrainingResultEntity
import com.lexicon.model.training.StepOutcome

class TrainingHistoryRepositoryImpl(
    private val trainingResultDao: TrainingResultDao,
) : TrainingHistoryRepository {
    override suspend fun recordResult(result: TrainingResultBoundary) {
        trainingResultDao.insert(
            TrainingResultEntity(
                sessionId = result.sessionId,
                trainingType = result.trainingType.id,
                stepIndex = result.stepIndex,
                vocabularyItemId = result.vocabularyItemId,
                expectedAnswer = result.expectedAnswer,
                submittedAnswer = result.submittedAnswer,
                outcome = result.outcome.name,
                tipUsed = result.tipUsed,
                completedAtEpochMillis = result.completedAtEpochMillis,
                wasReview = result.wasReview,
            ),
        )
    }

    override suspend fun lastAnsweredAtEpochMillis(): Long? = trainingResultDao.lastAnsweredAtEpochMillis()

    override suspend fun accuracyBetween(
        fromEpochMillis: Long,
        toEpochMillis: Long,
    ): AccuracyBoundary =
        AccuracyBoundary(
            answers = trainingResultDao.countAnswersBetween(SEEN, fromEpochMillis, toEpochMillis),
            correct = trainingResultDao.countCorrectBetween(CORRECT, fromEpochMillis, toEpochMillis),
        )

    override suspend fun countSessionsBetween(
        fromEpochMillis: Long,
        toEpochMillis: Long,
    ): Int = trainingResultDao.countSessionsBetween(fromEpochMillis, toEpochMillis)

    private companion object {
        val CORRECT = StepOutcome.CORRECT.name
        val SEEN = StepOutcome.SEEN.name
    }
}
