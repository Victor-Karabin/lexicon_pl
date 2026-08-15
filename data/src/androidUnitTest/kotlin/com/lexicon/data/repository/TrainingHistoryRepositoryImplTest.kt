package com.lexicon.data.repository

import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.boundary.TrainingResultOutcomeBoundary
import com.lexicon.common.Clock
import com.lexicon.data.local.StudyDayDao
import com.lexicon.data.local.TrainingResultDao
import com.lexicon.data.local.TrainingResultEntity
import com.lexicon.data.local.WordReviewDao
import com.lexicon.data.local.WordReviewEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Recording an answer is the one call every training already makes, so it is also
 * where the review schedule and the day's tally are kept up. These cover that the
 * three stay consistent with each other.
 */
class TrainingHistoryRepositoryImplTest {
    private val trainingResultDao: TrainingResultDao = mockk(relaxed = true)
    private val wordReviewDao: WordReviewDao = mockk(relaxed = true)
    private val studyDayDao: StudyDayDao = mockk(relaxed = true)
    private val clock = object : Clock {
        override fun nowEpochMillis(): Long = NOW

        override fun todayEpochDay(): Long = TODAY
    }

    private val repository = TrainingHistoryRepositoryImpl(trainingResultDao, wordReviewDao, studyDayDao, clock)

    private fun answer(
        wordId: Long = 1,
        outcome: TrainingResultOutcomeBoundary = TrainingResultOutcomeBoundary.CORRECT,
        tipUsed: Boolean = false,
        atEpochMillis: Long = NOW,
    ) = TrainingResultBoundary(
        sessionId = "s1",
        trainingType = "dictation",
        stepIndex = 0,
        vocabularyItemId = wordId,
        expectedAnswer = "woda",
        submittedAnswer = "woda",
        outcome = outcome,
        tipUsed = tipUsed,
        completedAtEpochMillis = atEpochMillis,
    )

    @Test
    fun `a word with no schedule is recorded as a first exposure and given one`() =
        runTest {
            coEvery { wordReviewDao.find(1) } returns null

            repository.recordResult(answer())

            val stored = slot<TrainingResultEntity>()
            coVerify { trainingResultDao.insert(capture(stored)) }
            assertFalse("a first exposure is not a review", stored.captured.wasReview)

            val schedule = slot<WordReviewEntity>()
            coVerify { wordReviewDao.upsert(capture(schedule)) }
            assertEquals(1, schedule.captured.repetitions)
            assertEquals(TODAY + 1, schedule.captured.dueAtEpochDay)

            coVerify { studyDayDao.record(TODAY, any(), wasNew = true, wasCorrect = true) }
        }

    @Test
    fun `a word that already had a schedule is recorded as a review`() =
        runTest {
            coEvery { wordReviewDao.find(1) } returns WordReviewEntity(
                wordId = 1,
                repetitions = 2,
                easeFactor = 2.5,
                intervalDays = 6,
                dueAtEpochDay = TODAY,
                lapses = 0,
                lastReviewedAtEpochMillis = NOW - 1000,
            )

            repository.recordResult(answer())

            val stored = slot<TrainingResultEntity>()
            coVerify { trainingResultDao.insert(capture(stored)) }
            assertTrue("a word with a schedule is a review", stored.captured.wasReview)
            coVerify { studyDayDao.record(TODAY, any(), wasNew = false, wasCorrect = true) }
        }

    @Test
    fun `a wrong answer sends the word back to the start of the ladder`() =
        runTest {
            coEvery { wordReviewDao.find(1) } returns WordReviewEntity(
                wordId = 1,
                repetitions = 4,
                easeFactor = 2.5,
                intervalDays = 30,
                dueAtEpochDay = TODAY,
                lapses = 0,
                lastReviewedAtEpochMillis = NOW - 1000,
            )

            repository.recordResult(answer(outcome = TrainingResultOutcomeBoundary.INCORRECT))

            val schedule = slot<WordReviewEntity>()
            coVerify { wordReviewDao.upsert(capture(schedule)) }
            assertEquals(0, schedule.captured.repetitions)
            assertEquals(1, schedule.captured.lapses)
            assertEquals(TODAY + 1, schedule.captured.dueAtEpochDay)
            coVerify { studyDayDao.record(TODAY, any(), any(), wasCorrect = false) }
        }

    @Test
    fun `a tip still moves the word forward, but costs it ease`() =
        runTest {
            coEvery { wordReviewDao.find(1) } returns null

            repository.recordResult(answer(tipUsed = true))

            val schedule = slot<WordReviewEntity>()
            coVerify { wordReviewDao.upsert(capture(schedule)) }
            assertEquals("a tipped answer still counts as recalled", 1, schedule.captured.repetitions)
            assertTrue("but it should not be free", schedule.captured.easeFactor < 2.5)
        }

    @Test
    fun `the first answer ever recorded is credited no time`() =
        runTest {
            coEvery { wordReviewDao.find(any()) } returns null
            coEvery { trainingResultDao.lastAnsweredAtEpochMillis() } returns null

            repository.recordResult(answer())

            coVerify { studyDayDao.record(TODAY, addedSeconds = 0, any(), any()) }
        }

    @Test
    fun `an answer is credited the time since the previous one`() =
        runTest {
            coEvery { wordReviewDao.find(any()) } returns null
            coEvery { trainingResultDao.lastAnsweredAtEpochMillis() } returns NOW - 15_000

            repository.recordResult(answer())

            coVerify { studyDayDao.record(TODAY, addedSeconds = 15, any(), any()) }
        }

    @Test
    fun `a long pause is not counted as studying`() =
        runTest {
            coEvery { wordReviewDao.find(any()) } returns null
            // An hour away from the phone between two answers.
            coEvery { trainingResultDao.lastAnsweredAtEpochMillis() } returns NOW - 3_600_000

            repository.recordResult(answer())

            coVerify { studyDayDao.record(TODAY, addedSeconds = 120, any(), any()) }
        }

    private companion object {
        const val TODAY = 20_000L
        const val NOW = 1_700_000_000_000L
    }
}
