package com.lexicon.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per day the learner studied, in their own time zone.
 *
 * A rollup rather than something derived on demand: streaks and weekly totals would
 * otherwise mean walking every training result ever recorded, and that set only
 * grows. It is written a field at a time as answers come in, so it cannot drift far
 * from the results it summarises.
 */
@Entity(tableName = "study_day")
data class StudyDayEntity(
    @PrimaryKey val epochDay: Long,
    val studiedSeconds: Long,
    /** Words answered for the first time. */
    val newWords: Int,
    /** Words answered that already had a schedule. */
    val reviews: Int,
    val answers: Int,
    val correctAnswers: Int,
)
