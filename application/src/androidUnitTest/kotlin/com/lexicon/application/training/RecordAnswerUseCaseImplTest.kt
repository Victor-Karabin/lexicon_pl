package com.lexicon.application.training

import com.lexicon.boundary.ReviewScheduleRepository
import com.lexicon.boundary.StudyRecordRepository
import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.common.Clock
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.model.scheduling.ReviewState
import com.lexicon.model.training.StepOutcome
import com.lexicon.model.training.TrainingType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordAnswerUseCaseImplTest {
    private val history: TrainingHistoryRepository = mockk(relaxed = true)
    private val reviews: ReviewScheduleRepository = mockk(relaxed = true)
    private val studyRecord: StudyRecordRepository = mockk(relaxed = true) {
        coEvery { lastActivityAtEpochMillis() } returns null
    }
    private val clock = object : Clock {
        override fun nowEpochMillis(): Long = NOW

        override fun todayEpochDay(): Long = TODAY
    }

    private val useCase = RecordAnswerUseCaseImpl(history, reviews, studyRecord, clock)

    private fun answer(
        wordId: Long = 1,
        outcome: StepOutcome = StepOutcome.CORRECT,
        tipUsed: Boolean = false,
    ) = RecordedAnswer(
        sessionId = "s1",
        trainingType = TrainingType.DICTATION,
        stepIndex = 0,
        vocabularyItemId = wordId,
        expectedAnswer = "woda",
        submittedAnswer = "woda",
        outcome = outcome,
        tipUsed = tipUsed,
    )

    @Test
    fun `a word with no schedule is recorded as a first exposure and given one`() =
        runTest {
            coEvery { reviews.find(1) } returns null

            useCase(answer())

            val stored = slot<TrainingResultBoundary>()
            coVerify { history.recordResult(capture(stored)) }
            assertFalse("a first exposure is not a review", stored.captured.wasReview)

            val schedule = slot<ReviewState>()
            coVerify { reviews.save(1, capture(schedule), NOW) }
            assertEquals(1, schedule.captured.repetitions)
            assertEquals(TODAY + 1, schedule.captured.dueAtEpochDay)

            coVerify { studyRecord.record(TODAY, any(), wasNew = true, wasCorrect = true, atEpochMillis = any()) }
        }

    @Test
    fun `a word that already had a schedule is recorded as a review`() =
        runTest {
            coEvery { reviews.find(1) } returns ReviewState(
                repetitions = 2,
                easeFactor = 2.5,
                intervalDays = 6,
                dueAtEpochDay = TODAY,
            )

            useCase(answer())

            val stored = slot<TrainingResultBoundary>()
            coVerify { history.recordResult(capture(stored)) }
            assertTrue("a word with a schedule is a review", stored.captured.wasReview)
            coVerify { studyRecord.record(TODAY, any(), wasNew = false, wasCorrect = true, atEpochMillis = any()) }
        }

    @Test
    fun `a wrong answer sends the word back to the start of the ladder`() =
        runTest {
            coEvery { reviews.find(1) } returns ReviewState(
                repetitions = 4,
                easeFactor = 2.5,
                intervalDays = 30,
                dueAtEpochDay = TODAY,
            )

            useCase(answer(outcome = StepOutcome.INCORRECT))

            val schedule = slot<ReviewState>()
            coVerify { reviews.save(1, capture(schedule), NOW) }
            assertEquals(0, schedule.captured.repetitions)
            assertEquals(1, schedule.captured.lapses)
            assertEquals(TODAY + 1, schedule.captured.dueAtEpochDay)
            coVerify { studyRecord.record(TODAY, any(), any(), wasCorrect = false, atEpochMillis = any()) }
        }

    @Test
    fun `a tip still moves the word forward, but costs it ease`() =
        runTest {
            coEvery { reviews.find(1) } returns null

            useCase(answer(tipUsed = true))

            val schedule = slot<ReviewState>()
            coVerify { reviews.save(1, capture(schedule), NOW) }
            assertEquals("a tipped answer still counts as recalled", 1, schedule.captured.repetitions)
            assertTrue("but it should not be free", schedule.captured.easeFactor < 2.5)
        }

    @Test
    fun `a word only shown is recorded but never scheduled`() =
        runTest {
            coEvery { reviews.find(1) } returns null

            useCase(answer(outcome = StepOutcome.SEEN))

            coVerify { history.recordResult(any()) }
            coVerify(exactly = 0) { reviews.save(any(), any(), any()) }
            coVerify(exactly = 0) { studyRecord.record(any(), any(), any(), any(), any()) }
            coVerify { studyRecord.recordTime(TODAY, any(), NOW) }
        }

    @Test
    fun `the first answer ever recorded is credited no time`() =
        runTest {
            coEvery { reviews.find(any()) } returns null
            coEvery { history.lastAnsweredAtEpochMillis() } returns null

            useCase(answer())

            coVerify { studyRecord.record(TODAY, addedSeconds = 0, any(), any(), atEpochMillis = any()) }
        }

    @Test
    fun `an answer is credited the time since the previous one`() =
        runTest {
            coEvery { reviews.find(any()) } returns null
            coEvery { history.lastAnsweredAtEpochMillis() } returns NOW - 15_000

            useCase(answer())

            coVerify { studyRecord.record(TODAY, addedSeconds = 15, any(), any(), atEpochMillis = any()) }
        }

    @Test
    fun `time is counted from the latest activity, whether an answer or a lesson`() =
        runTest {
            coEvery { reviews.find(any()) } returns null
            coEvery { history.lastAnsweredAtEpochMillis() } returns NOW - 90_000
            coEvery { studyRecord.lastActivityAtEpochMillis() } returns NOW - 20_000

            useCase(answer())

            coVerify { studyRecord.record(TODAY, addedSeconds = 20, any(), any(), atEpochMillis = NOW) }
        }

    @Test
    fun `a long pause is not counted as studying`() =
        runTest {
            coEvery { reviews.find(any()) } returns null

            coEvery { history.lastAnsweredAtEpochMillis() } returns NOW - 3_600_000

            useCase(answer())

            coVerify { studyRecord.record(TODAY, addedSeconds = 120, any(), any(), atEpochMillis = any()) }
        }

    private companion object {
        const val TODAY = 20_000L
        const val NOW = 1_700_000_000_000L
    }
}
