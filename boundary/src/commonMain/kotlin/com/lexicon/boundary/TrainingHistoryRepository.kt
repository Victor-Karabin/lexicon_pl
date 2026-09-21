package com.lexicon.boundary

interface TrainingHistoryRepository {
    suspend fun recordResult(result: TrainingResultBoundary)

    suspend fun lastAnsweredAtEpochMillis(): Long?
}
