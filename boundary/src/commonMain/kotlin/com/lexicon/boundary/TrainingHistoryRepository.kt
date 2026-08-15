package com.lexicon.boundary

/**
 * Where every answered step lands.
 *
 * [recordResult] is also what advances the review schedule and the day's record: it
 * is the one call every training already makes, so hanging the rest of the learning
 * record off it means none of them has to be changed or even know.
 */
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
