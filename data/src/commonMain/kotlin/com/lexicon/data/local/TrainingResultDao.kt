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

    @Query(
        """
        SELECT COUNT(*) FROM training_results
        WHERE completedAtEpochMillis BETWEEN :from AND :to
        """,
    )
    suspend fun countBetween(
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

    /**
     * Answers on words that had already been learned, which is what retention is
     * measured over — a first exposure says nothing about remembering.
     */
    @Query(
        """
        SELECT COUNT(*) FROM training_results
        WHERE wasReview = 1 AND completedAtEpochMillis BETWEEN :from AND :to
        """,
    )
    suspend fun countReviewsBetween(
        from: Long,
        to: Long,
    ): Int

    @Query(
        """
        SELECT COUNT(*) FROM training_results
        WHERE wasReview = 1 AND outcome = :correctOutcome
          AND completedAtEpochMillis BETWEEN :from AND :to
        """,
    )
    suspend fun countCorrectReviewsBetween(
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
}
