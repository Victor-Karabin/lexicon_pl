package com.lexicon.application.program

import com.lexicon.boundary.StudyDayBoundary
import com.lexicon.boundary.StudyRecordRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MILLIS_A_DAY = 86_400_000L
private const val TODAY = 20_000L

class GetDailyStudyTimeUseCaseImplTest {
    private val study: StudyRecordRepository = mockk()
    private val useCase = GetDailyStudyTimeUseCaseImpl(study, FixedClock(TODAY * MILLIS_A_DAY))

    private fun studied(vararg days: Pair<Long, Long>) {
        coEvery { study.daysBetween(any(), any()) } returns
            days.map { (epochDay, seconds) ->
                StudyDayBoundary(
                    epochDay = epochDay,
                    studiedSeconds = seconds,
                    newWords = 0,
                    reviews = 0,
                    answers = 1,
                    correctAnswers = 1,
                )
            }
    }

    @Test
    fun `the week ends today and reaches six days back`() =
        runTest {
            studied()

            val history = useCase()

            assertEquals((TODAY - 6..TODAY).toList(), history.days.map { it.epochDay })
            coVerify { study.daysBetween(TODAY - 6, TODAY) }
        }

    @Test
    fun `a day with nothing recorded comes back as zero rather than a gap`() =
        runTest {
            studied(TODAY to 600L, TODAY - 2 to 300L)

            val history = useCase()

            assertEquals(7, history.days.size)
            assertEquals(900L, history.totalSeconds)
            assertEquals(
                listOf(0L, 0L, 0L, 0L, 300L, 0L, 600L),
                history.days.map { it.studiedSeconds },
            )
        }

    @Test
    fun `the busiest day is the one that carries the chart`() =
        runTest {
            studied(TODAY to 600L, TODAY - 2 to 1800L)

            val history = useCase()

            assertEquals(TODAY - 2, history.busiest?.epochDay)
            assertEquals(1800L, history.busiestSeconds)
            assertEquals(1f, history.shareOfBusiest(history.days.single { it.epochDay == TODAY - 2 }), 0f)
            assertEquals(0.333f, history.shareOfBusiest(history.days.single { it.epochDay == TODAY }), 0.001f)
        }

    @Test
    fun `a week with nothing studied is still a full week of days`() =
        runTest {
            studied()

            val history = useCase()

            assertTrue(history.isEmpty)
            assertEquals(7, history.days.size)
            assertNull(history.busiest)
            assertEquals(0f, history.shareOfBusiest(history.days.first()), 0f)
        }

    @Test
    fun `less than a minute of study still reads as a minute`() =
        runTest {
            studied(TODAY to 20L)

            val history = useCase()

            val today = history.days.last()
            assertTrue(today.wasStudied)
            assertEquals(1, today.studiedMinutes)
            assertEquals(1, history.totalMinutes)
            assertFalse(history.isEmpty)
        }
}
