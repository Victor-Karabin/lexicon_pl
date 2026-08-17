package com.lexicon.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WordReviewDao {
    @Query("SELECT * FROM word_review WHERE wordId = :wordId")
    suspend fun find(wordId: Long): WordReviewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(review: WordReviewEntity)

    @Query(
        """
        SELECT r.wordId FROM word_review r
        INNER JOIN words w ON w.id = r.wordId
        WHERE r.dueAtEpochDay <= :todayEpochDay AND w.isDeleted = 0
        ORDER BY r.dueAtEpochDay ASC
        LIMIT :limit
        """,
    )
    suspend fun dueWordIds(
        todayEpochDay: Long,
        limit: Int,
    ): List<Long>

    @Query(
        """
        SELECT COUNT(*) FROM word_review r
        INNER JOIN words w ON w.id = r.wordId
        WHERE w.isDeleted = 0
        """,
    )
    suspend fun countLearned(): Int

    @Query(
        """
        SELECT COUNT(*) FROM word_review r
        INNER JOIN words w ON w.id = r.wordId
        WHERE r.intervalDays >= :masteredIntervalDays AND w.isDeleted = 0
        """,
    )
    suspend fun countMastered(masteredIntervalDays: Long): Int

    @Query("SELECT wordId FROM word_review")
    suspend fun allScheduledWordIds(): List<Long>
}
