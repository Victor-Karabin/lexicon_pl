package com.lexicon.data.repository

import com.lexicon.boundary.StudyDayBoundary
import com.lexicon.boundary.StudyRecordRepository
import com.lexicon.data.local.StudyDayDao
import com.lexicon.data.local.StudyDayEntity

class StudyRecordRepositoryImpl(
    private val studyDayDao: StudyDayDao,
) : StudyRecordRepository {
    override suspend fun day(epochDay: Long): StudyDayBoundary? = studyDayDao.find(epochDay)?.toBoundary()

    override suspend fun daysBetween(
        fromEpochDay: Long,
        toEpochDay: Long,
    ): List<StudyDayBoundary> = studyDayDao.between(fromEpochDay, toEpochDay).map { it.toBoundary() }

    /**
     * Counts back from the most recent day studied, stopping at the first gap.
     *
     * A streak survives today being empty — the day is not over — but not yesterday
     * being empty, which is a day genuinely missed. Starting from the newest day on
     * record rather than from today is what expresses that without a special case.
     */
    override suspend fun currentStreak(todayEpochDay: Long): Int {
        val days = studyDayDao.studiedDaysDescending()
        val mostRecent = days.firstOrNull() ?: return 0
        if (mostRecent < todayEpochDay - 1) return 0

        var streak = 0
        var expected = mostRecent
        for (day in days) {
            if (day != expected) break
            streak++
            expected--
        }
        return streak
    }
}

private fun StudyDayEntity.toBoundary(): StudyDayBoundary =
    StudyDayBoundary(
        epochDay = epochDay,
        studiedSeconds = studiedSeconds,
        newWords = newWords,
        reviews = reviews,
        answers = answers,
        correctAnswers = correctAnswers,
    )
