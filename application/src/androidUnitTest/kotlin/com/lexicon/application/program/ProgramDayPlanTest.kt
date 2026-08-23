package com.lexicon.application.program

import com.lexicon.boundary.ProgramDayBoundary
import com.lexicon.boundary.ProgramRepository
import com.lexicon.boundary.ReviewScheduleRepository
import com.lexicon.interactors.program.ActivityConfig
import com.lexicon.interactors.program.DailyPlanConfig
import com.lexicon.interactors.program.GetProgramUseCase
import com.lexicon.interactors.program.Program
import com.lexicon.interactors.program.ProgramConfig
import com.lexicon.interactors.program.ProgramDifficulty
import com.lexicon.interactors.program.ProgramVisibility
import com.lexicon.interactors.program.ResolveProgramScopeUseCase
import com.lexicon.model.program.ActivityType
import com.lexicon.model.program.ProgramId
import com.lexicon.model.vocabulary.LocalizedText
import com.lexicon.model.vocabulary.VocabularyId
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val TODAY_MILLIS = 1_700_000_000_000
private const val NEW_WORDS_A_DAY = 3

class ProgramDayPlanTest {
    private val id = ProgramId("mine")
    private val clock = FixedClock(TODAY_MILLIS)
    private val today = clock.todayEpochDay()

    private val programs: ProgramRepository = mockk()
    private val reviews: ReviewScheduleRepository = mockk()
    private val getProgram: GetProgramUseCase = mockk()
    private val resolveScope: ResolveProgramScopeUseCase = mockk()

    private var scope: List<Long> = (1L..10L).toList()
    private var scheduled: Set<Long> = emptySet()
    private var due: List<Long> = emptyList()
    private var stored: ProgramDayBoundary? = null

    private val getDay = GetProgramDayUseCaseImpl(getProgram, resolveScope, programs, reviews, clock)

    private val startSession = StartProgramSessionUseCaseImpl(getProgram, getDay, resolveScope, reviews, clock)

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
                    reviewWords = 5,
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
        coEvery { resolveScope(any()) } answers { scope.map(::VocabularyId).toImmutableList() }
        coEvery { reviews.scheduledWordIds() } answers { scheduled }
        coEvery { reviews.dueWordIds(any(), any()) } answers { due }
        coEvery { programs.day(id.value, today) } answers { stored }
        coEvery { programs.saveDay(any()) } answers { stored = firstArg() }
    }

    @Test
    fun `the day is the first unscheduled words in the scope`() =
        runTest {
            given()

            assertEquals(listOf(1L, 2L, 3L), getDay(id)?.newWords?.map { it.value })
        }

    @Test
    fun `the day survives the words it taught becoming scheduled`() =
        runTest {
            given()
            val first = getDay(id)?.newWords

            scheduled = setOf(1L, 2L, 3L)

            assertEquals(first, getDay(id)?.newWords)
        }

    @Test
    fun `changing the study set replans the day`() =
        runTest {
            given()
            assertEquals(listOf(1L, 2L, 3L), getDay(id)?.newWords?.map { it.value })

            scope = (100L..110L).toList()

            assertEquals(listOf(100L, 101L, 102L), getDay(id)?.newWords?.map { it.value })
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
    fun `every training in the day gets the day's words`() =
        runTest {
            given()

            val words = startSession(id)?.wordIds?.map { it.value }
            scheduled = setOf(1L, 2L, 3L)

            assertEquals(words, startSession(id)?.wordIds?.map { it.value })
        }

    @Test
    fun `words due for review join the day's new ones`() =
        runTest {
            given()
            due = listOf(7L, 8L)

            assertEquals(listOf(1L, 2L, 3L, 7L, 8L), startSession(id)?.wordIds?.map { it.value })
        }

    @Test
    fun `a due word already being taught today is not repeated`() =
        runTest {
            given()
            due = listOf(2L, 8L)

            assertEquals(listOf(1L, 2L, 3L, 8L), startSession(id)?.wordIds?.map { it.value })
        }

    @Test
    fun `a day with nothing left to teach or review has no session`() =
        runTest {
            given()
            scope = emptyList()

            assertNull(startSession(id))
        }
}
