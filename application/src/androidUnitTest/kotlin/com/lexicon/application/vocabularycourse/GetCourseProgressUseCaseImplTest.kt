package com.lexicon.application.vocabularycourse

import com.lexicon.boundary.ReviewScheduleRepository
import com.lexicon.boundary.StudyRecordRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.common.Clock
import com.lexicon.model.scheduling.RecallQuality
import com.lexicon.model.scheduling.ReviewState
import com.lexicon.model.scheduling.next
import com.lexicon.model.vocabulary.WordStatus
import com.lexicon.model.vocabularycourse.ProgressMetricType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetCourseProgressUseCaseImplTest {
    private val vocabulary: VocabularyRepository = mockk {
        coEvery { countWithStatus(WordStatus.KNOWN) } returns KNOWN
        coEvery { countWithStatus(WordStatus.TO_LEARN) } returns TO_LEARN
        coEvery { countWithStatus(WordStatus.FAVOURITE) } returns FAVOURITE
    }
    private val study: StudyRecordRepository = mockk { coEvery { day(TODAY) } returns null }
    private val reviews: ReviewScheduleRepository = mockk()
    private val clock = object : Clock {
        override fun nowEpochMillis(): Long = 0

        override fun todayEpochDay(): Long = TODAY
    }

    private val progress = GetCourseProgressUseCaseImpl(vocabulary = vocabulary, study = study, reviews = reviews, clock = clock)

    private fun scheduledFor(dueAt: Long) = ReviewState(repetitions = 2, intervalDays = 6, dueAtEpochDay = dueAt)

    @Test
    fun `the due count is the known words whose review has come round`() =
        runTest {
            coEvery { reviews.knownWordSchedules() } returns mapOf(
                1L to scheduledFor(TODAY - 5),
                2L to scheduledFor(TODAY),
                3L to scheduledFor(TODAY + 1),
                4L to ReviewState().next(RecallQuality.PERFECT, TODAY),
                5L to ReviewState(repetitions = 3, intervalDays = 20).next(RecallQuality.FORGOTTEN, TODAY),
            )

            assertEquals(2, progress().dueWords)
        }

    @Test
    fun `nothing is due when no known word has a schedule`() =
        runTest {
            coEvery { reviews.knownWordSchedules() } returns emptyMap()

            assertEquals(0, progress().dueWords)
        }

    @Test
    fun `the vocabulary metric still counts known words out of known and learning`() =
        runTest {
            coEvery { reviews.knownWordSchedules() } returns emptyMap()

            val metric = checkNotNull(progress().metric(ProgressMetricType.VOCABULARY))

            assertEquals(KNOWN, metric.current)
            assertEquals(KNOWN + TO_LEARN + FAVOURITE, metric.target)
        }

    private companion object {
        const val TODAY = 20_000L
        const val KNOWN = 40
        const val TO_LEARN = 5
        const val FAVOURITE = 2
    }
}
