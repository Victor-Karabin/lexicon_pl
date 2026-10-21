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
        SELECT r.* FROM word_review r
        INNER JOIN words w ON w.id = r.wordId
        WHERE w.status = 'KNOWN' AND w.isDeleted = 0
        """,
    )
    suspend fun knownSchedules(): List<WordReviewEntity>
}
