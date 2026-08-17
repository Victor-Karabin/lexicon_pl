package com.lexicon.boundary

interface TrainingHistoryRepository {
    suspend fun recordResult(result: TrainingResultBoundary)

    suspend fun accuracyBetween(
        fromEpochMillis: Long,
        toEpochMillis: Long,
    ): AccuracyBoundary

    suspend fun countSessionsBetween(
        fromEpochMillis: Long,
        toEpochMillis: Long,
    ): Int

    suspend fun countSessionsOfTrainingBetween(
        trainingType: String,
        fromEpochMillis: Long,
        toEpochMillis: Long,
    ): Int

    suspend fun resultsForWord(wordId: Long): List<TrainingResultBoundary>
}
