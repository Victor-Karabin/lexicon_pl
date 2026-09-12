package com.lexicon.application.program

import com.lexicon.boundary.StudyRecordRepository
import com.lexicon.common.Clock
import com.lexicon.interactors.program.DailyStudyTime
import com.lexicon.interactors.program.GetDailyStudyTimeUseCase
import com.lexicon.interactors.program.StudyTimeHistory
import kotlinx.collections.immutable.toImmutableList

private const val DAYS_SHOWN = 7

class GetDailyStudyTimeUseCaseImpl(
    private val study: StudyRecordRepository,
    private val clock: Clock,
) : GetDailyStudyTimeUseCase {
    /**
     * A day only gets a row once something was answered on it, so the week is rebuilt
     * from today backwards and the days that are missing come back as zero rather than
     * as gaps the chart would have to guess at.
     */
    override suspend fun invoke(): StudyTimeHistory {
        val today = clock.todayEpochDay()
        val from = today - DAYS_SHOWN + 1
        val studied = study.daysBetween(from, today).associate { it.epochDay to it.studiedSeconds }

        return StudyTimeHistory(
            days = (from..today)
                .map { DailyStudyTime(epochDay = it, studiedSeconds = studied[it] ?: 0L) }
                .toImmutableList(),
        )
    }
}
