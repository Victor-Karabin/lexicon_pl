package com.lexicon.application.training

import com.lexicon.boundary.StudyRecordRepository
import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.common.Clock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RecordStudyActivityUseCaseImplTest {
    private val history: TrainingHistoryRepository = mockk { coEvery { lastAnsweredAtEpochMillis() } returns null }
    private val studyRecord: StudyRecordRepository = mockk(relaxed = true) { coEvery { lastActivityAtEpochMillis() } returns null }
    private val clock = object : Clock {
        override fun nowEpochMillis(): Long = NOW

        override fun todayEpochDay(): Long = TODAY
    }
    private val record = RecordStudyActivityUseCaseImpl(history, studyRecord, clock)

    @Test
    fun `the first activity marks the start and is credited no time`() =
        runTest {
            record()
            coVerify { studyRecord.recordTime(TODAY, addedSeconds = 0, atEpochMillis = NOW) }
        }

    @Test
    fun `an activity is credited the time since the last one, answer or lesson`() =
        runTest {
            coEvery { studyRecord.lastActivityAtEpochMillis() } returns NOW - 40_000
            coEvery { history.lastAnsweredAtEpochMillis() } returns NOW - 90_000
            record()
            coVerify { studyRecord.recordTime(TODAY, addedSeconds = 40, atEpochMillis = NOW) }
        }

    @Test
    fun `a long pause is credited at most two minutes`() =
        runTest {
            coEvery { studyRecord.lastActivityAtEpochMillis() } returns NOW - 3_600_000
            record()
            coVerify { studyRecord.recordTime(TODAY, addedSeconds = 120, atEpochMillis = NOW) }
        }

    @Test
    fun `activity within a few seconds of the last is not written, so typing is cheap and loses no time`() =
        runTest {
            coEvery { studyRecord.lastActivityAtEpochMillis() } returns NOW - 3_000
            record()
            coVerify(exactly = 0) { studyRecord.recordTime(any(), any(), any()) }
        }

    private companion object {
        const val TODAY = 20_000L
        const val NOW = 1_700_000_000_000L
    }
}
