package com.lexicon.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/** One row per answered step, and the reads every learning metric is built from. */
@Dao
interface TrainingResultDao {
    @Insert
    suspend fun insert(result: TrainingResultEntity)

    @Query("SELECT * FROM training_results WHERE vocabularyItemId = :wordId ORDER BY completedAtEpochMillis")
    suspend fun forWord(wordId: Long): List<TrainingResultEntity>

    /** Answered steps only: a word that was shown and not asked about is not one. */
    @Query(
        """
        SELECT COUNT(*) FROM training_results
        WHERE outcome != :shownOutcome AND completedAtEpochMillis BETWEEN :from AND :to
        """,
    )
    suspend fun countAnswersBetween(
        shownOutcome: String,
        from: Long,
        to: Long,
    ): Int

    @Query(
        """
        SELECT COUNT(*) FROM training_results
        WHERE outcome = :correctOutcome AND completedAtEpochMillis BETWEEN :from AND :to
        """,
    )
    suspend fun countCorrectBetween(
        correctOutcome: String,
        from: Long,
        to: Long,
    ): Int

    @Query("SELECT MAX(completedAtEpochMillis) FROM training_results")
    suspend fun lastAnsweredAtEpochMillis(): Long?

    @Query("SELECT COUNT(DISTINCT sessionId) FROM training_results WHERE completedAtEpochMillis BETWEEN :from AND :to")
    suspend fun countSessionsBetween(
        from: Long,
        to: Long,
    ): Int

    /**
     * Sessions of one training in a window.
     *
     * A program's queue reads its own progress from this rather than being ticked
     * off as the learner goes: the work counts because it was answered, wherever it
     * was started from.
     */
    @Query(
        """
        SELECT COUNT(DISTINCT sessionId) FROM training_results
        WHERE trainingType = :trainingType AND completedAtEpochMillis BETWEEN :from AND :to
        """,
    )
    suspend fun countSessionsOfTrainingBetween(
        trainingType: String,
        from: Long,
        to: Long,
    ): Int
}
