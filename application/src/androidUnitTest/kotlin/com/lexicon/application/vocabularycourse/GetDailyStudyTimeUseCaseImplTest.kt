package com.lexicon.application.vocabularycourse

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

    @Test
    fun `study time rounds to the nearest minute and never reads below one for a studied day`() =
        runTest {
            val seconds = listOf(0L, 29L, 30L, 89L, 90L, 150L)
            studied(*seconds.mapIndexed { index, value -> TODAY - index to value }.toTypedArray())

            val history = useCase()

            val minutesBySeconds = history.days.associate { it.studiedSeconds to it.studiedMinutes }
            assertEquals(0, minutesBySeconds.getValue(0L))
            assertEquals(1, minutesBySeconds.getValue(29L))
            assertEquals(1, minutesBySeconds.getValue(30L))
            assertEquals(1, minutesBySeconds.getValue(89L))
            assertEquals(2, minutesBySeconds.getValue(90L))
            assertEquals(3, minutesBySeconds.getValue(150L))
        }

    @Test
    fun `the total rounds the summed seconds rather than the daily minutes`() =
        runTest {
            studied(TODAY to 40L, TODAY - 1 to 40L, TODAY - 2 to 40L)

            val history = useCase()

            assertEquals(120L, history.totalSeconds)
            assertEquals(2, history.totalMinutes)
            assertEquals(listOf(1, 1, 1), history.days.filter { it.wasStudied }.map { it.studiedMinutes })
        }

    @Test
    fun `a week with nothing studied totals zero minutes`() =
        runTest {
            studied()

            val history = useCase()

            assertEquals(0, history.totalMinutes)
            assertEquals(0, history.days.first().studiedMinutes)
        }
}
