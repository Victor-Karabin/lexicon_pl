package com.lexicon.application.training

import com.lexicon.boundary.StudyRecordRepository
import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.common.Clock
import com.lexicon.interactors.training.RecordStudyActivityUseCase
import com.lexicon.model.scheduling.StudyTimePolicy

private const val QUIET_MILLIS = 15_000L

class RecordStudyActivityUseCaseImpl(
    private val history: TrainingHistoryRepository,
    private val studyRecord: StudyRecordRepository,
    private val clock: Clock,
    private val studyTimePolicy: StudyTimePolicy = StudyTimePolicy(),
) : RecordStudyActivityUseCase {
    override suspend fun invoke() {
        val now = clock.nowEpochMillis()
        val previous = listOfNotNull(history.lastAnsweredAtEpochMillis(), studyRecord.lastActivityAtEpochMillis()).maxOrNull()
        if (previous != null && now - previous in 0 until QUIET_MILLIS) return
        studyRecord.recordTime(
            epochDay = clock.todayEpochDay(),
            addedSeconds = studyTimePolicy.creditedSeconds(now, previous),
            atEpochMillis = now,
        )
    }
}
