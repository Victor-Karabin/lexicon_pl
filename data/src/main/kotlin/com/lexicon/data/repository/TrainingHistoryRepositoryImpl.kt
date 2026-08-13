package com.lexicon.data.repository

import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.data.local.TrainingResultDao
import com.lexicon.data.local.TrainingResultEntity

class TrainingHistoryRepositoryImpl(
    private val trainingResultDao: TrainingResultDao,
) : TrainingHistoryRepository {
    override suspend fun recordResult(result: TrainingResultBoundary) {
        trainingResultDao.insert(
            TrainingResultEntity(
                sessionId = result.sessionId,
                trainingType = result.trainingType,
                stepIndex = result.stepIndex,
                vocabularyItemId = result.vocabularyItemId,
                expectedAnswer = result.expectedAnswer,
                submittedAnswer = result.submittedAnswer,
                outcome = result.outcome.name,
                tipUsed = result.tipUsed,
                completedAtEpochMillis = result.completedAtEpochMillis,
            ),
        )
    }
}
