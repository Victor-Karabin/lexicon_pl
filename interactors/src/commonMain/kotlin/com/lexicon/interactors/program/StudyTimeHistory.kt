package com.lexicon.interactors.program

import kotlinx.collections.immutable.ImmutableList

private const val SECONDS_PER_MINUTE = 60

data class DailyStudyTime(
    val epochDay: Long,
    val studiedSeconds: Long,
) {
    val wasStudied: Boolean get() = studiedSeconds > 0

    /**
     * A minute of study is worth showing, so anything short of one still reads as one
     * rather than rounding away to nothing.
     */
    val studiedMinutes: Int
        get() = when {
            studiedSeconds <= 0 -> 0
            else -> maxOf(1, ((studiedSeconds + SECONDS_PER_MINUTE / 2) / SECONDS_PER_MINUTE).toInt())
        }
}

data class StudyTimeHistory(
    val days: ImmutableList<DailyStudyTime>,
) {
    val totalSeconds: Long get() = days.sumOf { it.studiedSeconds }

    val busiestSeconds: Long get() = days.maxOfOrNull { it.studiedSeconds } ?: 0

    val isEmpty: Boolean get() = totalSeconds == 0L

    val totalMinutes: Int
        get() = when {
            totalSeconds <= 0 -> 0
            else -> maxOf(1, ((totalSeconds + SECONDS_PER_MINUTE / 2) / SECONDS_PER_MINUTE).toInt())
        }

    val busiest: DailyStudyTime? get() = days.maxByOrNull { it.studiedSeconds }?.takeIf { it.wasStudied }

    fun shareOfBusiest(day: DailyStudyTime): Float =
        if (busiestSeconds <= 0) 0f else (day.studiedSeconds.toDouble() / busiestSeconds).toFloat()
}
