package com.lexicon.pl.data.local

import androidx.room.Dao
import androidx.room.Insert

@Dao
interface TrainingResultDao {
    @Insert
    suspend fun insert(result: TrainingResultEntity)
}
