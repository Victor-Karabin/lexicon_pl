package com.lexicon.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WordDao {
    @Query("SELECT * FROM words ORDER BY RANDOM() LIMIT :count")
    suspend fun getRandom(count: Int): List<WordEntity>

    @Query("SELECT * FROM words WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<WordEntity>

    @Query("SELECT COUNT(*) FROM words")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(words: List<WordEntity>)
}
