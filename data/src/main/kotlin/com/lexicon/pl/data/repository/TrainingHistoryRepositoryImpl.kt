package com.lexicon.pl.data.repository

import com.lexicon.pl.boundary.TrainingHistoryRepository
import com.lexicon.pl.boundary.TrainingResultBoundary
import com.lexicon.pl.data.local.TrainingResultDao
import com.lexicon.pl.data.local.TrainingResultEntity
import javax.inject.Inject

class TrainingHistoryRepositoryImpl @Inject constructor(
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
