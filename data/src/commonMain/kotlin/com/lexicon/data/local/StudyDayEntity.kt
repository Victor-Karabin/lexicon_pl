package com.lexicon.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_day")
data class StudyDayEntity(
    @PrimaryKey val epochDay: Long,
    val studiedSeconds: Long,
    val newWords: Int,
    val reviews: Int,
    val answers: Int,
    val correctAnswers: Int,
)
