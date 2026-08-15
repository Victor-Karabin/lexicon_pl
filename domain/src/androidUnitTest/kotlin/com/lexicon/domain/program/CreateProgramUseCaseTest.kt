package com.lexicon.domain.program

import com.lexicon.boundary.ProgramBoundary
import com.lexicon.boundary.ProgramRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.common.Clock
import com.lexicon.interactors.program.ActivityType
import com.lexicon.interactors.program.ProgramDraft
import com.lexicon.interactors.program.ProgramDraftException
import com.lexicon.interactors.program.ProgramDraftProblem
import com.lexicon.interactors.program.ScopeSourceType
import com.lexicon.interactors.program.TargetType
import com.lexicon.interactors.program.trainingsADay
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

private const val FAVOURITES = 84

/**
 * A program the learner writes has to come out as ordinary configuration: the engine
 * runs it without knowing who wrote it, so what the form does not ask for still has
 * to be filled in correctly.
 */
class CreateProgramUseCaseTest {
    private val programs: ProgramRepository = mockk()
    private val vocabulary: VocabularyRepository = mockk()
    private val clock = FixedClock(nowMillis = 1_700_000_000_000)
    private val saved = slot<ProgramBoundary>()

    private val createProgram = CreateProgramUseCaseImpl(programs, vocabulary, clock)

    @Before
    fun setUp() {
        coEvery { vocabulary.favouriteWordIds() } returns List(FAVOURITES) { it.toLong() }
        coJustRun { programs.saveProgram(capture(saved)) }
    }

    private val draft = ProgramDraft(
        title = "  My starred words  ",
        description = "The ones I keep forgetting",
        newWordsPerDay = 15,
        reviewWordsPerDay = 30,
        // The same training twice is two turns at it, which is how a day gets its length.
        trainings = listOf("word_match", "dictation", "word_match"),
    )

    @Test
    fun `the program is stored under the id it was given`() =
        runTest {
            val program = createProgram(draft).getOrThrow()

            assertEquals(program.id.value, saved.captured.id)
            // Trimmed, so a stray space does not become part of the name.
            assertEquals("My starred words", saved.captured.title["en"])
        }

    @Test
    fun `the goal is the study set the program teaches`() =
        runTest {
            val config = createProgram(draft).getOrThrow().config

            // Nothing but favourites, so the program cannot quietly teach something else.
            assertEquals(ScopeSourceType.FAVOURITES, config.scope.include.single().type)
            assertEquals(FAVOURITES, config.goals.first { it.type == TargetType.VOCABULARY }.target)
        }

    @Test
    fun `the queue is the day, turn for turn and in order`() =
        runTest {
            val plan = createProgram(draft).getOrThrow().config.dailyPlan

            assertEquals(listOf("word_match", "dictation", "word_match"), plan.queue)
            // Three turns, because the queue lists three — nothing is doubled behind it.
            assertEquals(3, plan.trainingsADay)
            assertEquals(15, plan.newWords)
            assertEquals(30, plan.reviewWords)
        }

    @Test
    fun `both activities draw on the queue, so reviews have somewhere to run`() =
        runTest {
            val activities = createProgram(draft).getOrThrow().config.dailyPlan.activities

            assertEquals(setOf(ActivityType.LEARN, ActivityType.REVIEW), activities.map { it.type }.toSet())
            // The distinct ones: an activity names what can satisfy it, not how often.
            activities.forEach { assertEquals(draft.trainings.distinct(), it.trainings) }
        }

    @Test
    fun `a day cannot ask for more words than the study set holds`() =
        runTest {
            val plan = createProgram(
                draft.copy(newWordsPerDay = FAVOURITES * 2, reviewWordsPerDay = FAVOURITES * 2),
            ).getOrThrow().config.dailyPlan

            assertEquals(FAVOURITES, plan.newWords)
            assertEquals(FAVOURITES, plan.reviewWords)
        }

    @Test
    fun `a draft without a name is refused`() =
        runTest {
            assertEquals(ProgramDraftProblem.MISSING_TITLE, problemOf(draft.copy(title = "   ")))
        }

    @Test
    fun `a draft with no trainings is refused, because the day would be empty`() =
        runTest {
            assertEquals(ProgramDraftProblem.NO_TRAININGS, problemOf(draft.copy(trainings = emptyList())))
        }

    @Test
    fun `a draft over an empty study set is refused, because it would teach nothing`() =
        runTest {
            coEvery { vocabulary.favouriteWordIds() } returns emptyList()

            assertEquals(ProgramDraftProblem.NO_FAVOURITES, problemOf(draft))
            assertFalse(saved.isCaptured)
        }

    private suspend fun problemOf(draft: ProgramDraft): ProgramDraftProblem =
        (createProgram(draft).exceptionOrNull() as ProgramDraftException).problem
}

private class FixedClock(private val nowMillis: Long) : Clock {
    override fun nowEpochMillis(): Long = nowMillis

    override fun todayEpochDay(): Long = nowMillis / 86_400_000L
}
