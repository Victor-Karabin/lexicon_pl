package com.lexicon.interactors.vocabularycourse

import kotlinx.collections.immutable.ImmutableList

private const val SECONDS_PER_MINUTE = 60

private fun minutesOf(seconds: Long): Int =
    if (seconds <= 0) 0 else maxOf(1, ((seconds + SECONDS_PER_MINUTE / 2) / SECONDS_PER_MINUTE).toInt())

data class DailyStudyTime(
    val epochDay: Long,
    val studiedSeconds: Long,
) {
    val wasStudied: Boolean get() = studiedSeconds > 0

    val studiedMinutes: Int get() = minutesOf(studiedSeconds)
}

data class StudyTimeHistory(
    val days: ImmutableList<DailyStudyTime>,
) {
    val totalSeconds: Long get() = days.sumOf { it.studiedSeconds }

    val busiestSeconds: Long get() = days.maxOfOrNull { it.studiedSeconds } ?: 0

    val isEmpty: Boolean get() = totalSeconds == 0L

    val totalMinutes: Int get() = minutesOf(totalSeconds)

    val busiest: DailyStudyTime? get() = days.maxByOrNull { it.studiedSeconds }?.takeIf { it.wasStudied }

    fun shareOfBusiest(day: DailyStudyTime): Float =
        if (busiestSeconds <= 0) 0f else (day.studiedSeconds.toDouble() / busiestSeconds).toFloat()
}
