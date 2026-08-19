package com.lexicon.boundary

interface TrainingHistoryRepository {
    suspend fun recordResult(result: TrainingResultBoundary)

    suspend fun lastAnsweredAtEpochMillis(): Long?

    suspend fun accuracyBetween(
        fromEpochMillis: Long,
        toEpochMillis: Long,
    ): AccuracyBoundary

    suspend fun countSessionsBetween(
        fromEpochMillis: Long,
        toEpochMillis: Long,
    ): Int
}
