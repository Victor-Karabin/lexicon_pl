package com.lexicon.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface StudyDayDao {
    @Query("SELECT * FROM study_day WHERE epochDay = :epochDay")
    suspend fun find(epochDay: Long): StudyDayEntity?

    @Query("SELECT * FROM study_day WHERE epochDay BETWEEN :from AND :to ORDER BY epochDay")
    suspend fun between(
        from: Long,
        to: Long,
    ): List<StudyDayEntity>

    @Query("SELECT epochDay FROM study_day ORDER BY epochDay DESC")
    suspend fun studiedDaysDescending(): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(day: StudyDayEntity)

    @Transaction
    suspend fun record(
        epochDay: Long,
        addedSeconds: Long,
        wasNew: Boolean,
        wasCorrect: Boolean,
    ) {
        val current = find(epochDay) ?: StudyDayEntity(
            epochDay = epochDay,
            studiedSeconds = 0,
            newWords = 0,
            reviews = 0,
            answers = 0,
            correctAnswers = 0,
        )
        upsert(
            current.copy(
                studiedSeconds = current.studiedSeconds + addedSeconds,
                newWords = current.newWords + if (wasNew) 1 else 0,
                reviews = current.reviews + if (wasNew) 0 else 1,
                answers = current.answers + 1,
                correctAnswers = current.correctAnswers + if (wasCorrect) 1 else 0,
            ),
        )
    }
}
