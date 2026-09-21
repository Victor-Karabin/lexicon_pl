package com.lexicon.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TrainingResultDao {
    @Insert
    suspend fun insert(result: TrainingResultEntity)

    @Query("SELECT MAX(completedAtEpochMillis) FROM training_results")
    suspend fun lastAnsweredAtEpochMillis(): Long?
}
