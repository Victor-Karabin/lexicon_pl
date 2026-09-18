package com.lexicon.application.program

import com.lexicon.boundary.ProgramDayBoundary
import com.lexicon.boundary.ProgramRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.program.ActivityConfig
import com.lexicon.interactors.program.DailyPlanConfig
import com.lexicon.interactors.program.GetProgramUseCase
import com.lexicon.interactors.program.Program
import com.lexicon.interactors.program.ProgramConfig
import com.lexicon.interactors.program.ProgramDifficulty
import com.lexicon.interactors.program.ProgramVisibility
import com.lexicon.model.program.ActivityType
import com.lexicon.model.program.ProgramId
import com.lexicon.model.vocabulary.LocalizedText
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val TODAY_MILLIS = 1_700_000_000_000
private const val NEW_WORDS_A_DAY = 3
private const val REVIEW_WORDS_A_DAY = 5

class ProgramDayPlanTest {
    private val id = ProgramId("mine")
    private val clock = FixedClock(TODAY_MILLIS)
    private val today = clock.todayEpochDay()

    private val programs: ProgramRepository = mockk()
    private val vocabulary: VocabularyRepository = mockk()
    private val getProgram: GetProgramUseCase = mockk()

    private var learning: List<Long> = (1L..10L).toList()
    private var known: List<Long> = emptyList()
    private var stored: ProgramDayBoundary? = null

    private val getDay = GetProgramDayUseCaseImpl(getProgram, programs, vocabulary, clock)

    private val startSession = StartProgramSessionUseCaseImpl(getProgram, getDay, vocabulary)

    private fun program(queue: List<String>) =
        Program(
            id = id,
            level = "",
            order = 0,
            title = LocalizedText(mapOf("en" to "Mine")),
            description = LocalizedText(emptyMap()),
            difficulty = ProgramDifficulty.BEGINNER,
            estimatedDays = 0,
            visibility = ProgramVisibility.PRIVATE,
            config = ProgramConfig(
                dailyPlan = DailyPlanConfig(
                    newWords = NEW_WORDS_A_DAY,
                    reviewWords = REVIEW_WORDS_A_DAY,
                    queue = queue,
                    activities = listOf(
                        ActivityConfig(id = "learn", type = ActivityType.LEARN, trainings = queue),
                        ActivityConfig(id = "review", type = ActivityType.REVIEW, trainings = queue),
                    ),
                ),
            ),
        )

    private fun given(queue: List<String> = listOf("dictation", "puzzle")) {
        coEvery { getProgram(id) } returns program(queue)
        coEvery { vocabulary.learningWordIds(any()) } answers { learning.take(firstArg()) }
        coEvery { vocabulary.randomKnownWordIds(any()) } answers { known.take(firstArg()) }
        coEvery { programs.day(id.value, today) } answers { stored }
        coEvery { programs.saveDay(any()) } answers { stored = firstArg() }
    }

    @Test
    fun `the day teaches as many words to learn as the plan asks for`() =
        runTest {
            given()

            assertEquals(listOf(1L, 2L, 3L), getDay(id)?.newWords?.map { it.value })
        }

    @Test
    fun `a word whose status changed is swapped for the next one straight away`() =
        runTest {
            given()
            assertEquals(listOf(1L, 2L, 3L), getDay(id)?.newWords?.map { it.value })

            learning = listOf(2L, 3L, 4L, 5L)

            assertEquals(listOf(2L, 3L, 4L), getDay(id)?.newWords?.map { it.value })
        }

    @Test
    fun `changing the queue replans the day`() =
        runTest {
            given()
            val before = stored.also { getDay(id) }

            given(queue = listOf("puzzle", "dictation", "crossword"))
            getDay(id)

            assertNotEquals(before?.activitiesJson, stored?.activitiesJson)
        }

    @Test
    fun `a session is the words being learned and known ones alongside them`() =
        runTest {
            given()
            known = listOf(7L, 8L)

            val words = startSession(id)?.wordIds?.map { it.value }

            assertEquals(listOf(1L, 2L, 3L, 7L, 8L), words?.sorted())
        }

    @Test
    fun `a known word already being taught is not asked twice`() =
        runTest {
            given()
            known = listOf(2L, 8L)

            val words = startSession(id)?.wordIds?.map { it.value }

            assertEquals(listOf(1L, 2L, 3L, 8L), words?.sorted())
        }

    @Test
    fun `no more known words than the plan asks for`() =
        runTest {
            given()
            known = (20L..40L).toList()

            val words = startSession(id)?.wordIds?.map { it.value }

            assertEquals(NEW_WORDS_A_DAY + REVIEW_WORDS_A_DAY, words?.size)
        }

    @Test
    fun `nothing to learn and nothing known means no session`() =
        runTest {
            given()
            learning = emptyList()
            known = emptyList()

            assertNull(startSession(id))
        }

    @Test
    fun `known words alone still make a session`() =
        runTest {
            given()
            learning = emptyList()
            known = listOf(5L, 6L)

            assertEquals(listOf(5L, 6L), startSession(id)?.wordIds?.map { it.value }?.sorted())
        }
}
