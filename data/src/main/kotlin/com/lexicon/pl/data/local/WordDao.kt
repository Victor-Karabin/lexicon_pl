package com.lexicon.pl.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WordDao {
    @Query("SELECT * FROM words ORDER BY RANDOM() LIMIT :count")
    suspend fun getRandom(count: Int): List<WordEntity>

    @Insert
    suspend fun insertAll(words: List<WordEntity>)
}
