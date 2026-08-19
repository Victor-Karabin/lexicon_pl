package com.lexicon.application.program

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

private const val STUDY_SET = 84

class CreateProgramUseCaseTest {
    private val programs: ProgramRepository = mockk()
    private val vocabulary: VocabularyRepository = mockk()
    private val clock = FixedClock(nowMillis = 1_700_000_000_000)
    private val saved = slot<ProgramBoundary>()

    private val createProgram = CreateProgramUseCaseImpl(programs, vocabulary, clock)

    @Before
    fun setUp() {
        coEvery { vocabulary.studySetWordIds() } returns List(STUDY_SET) { it.toLong() }
        coJustRun { programs.saveProgram(capture(saved)) }
    }

    private val draft = ProgramDraft(
        title = "  My studySet  ",
        description = "The ones I keep forgetting",
        newWordsPerDay = 15,
        reviewWordsPerDay = 30,
        trainings = listOf("word_match", "dictation", "word_match"),
    )

    @Test
    fun `the program is stored under the id it was given`() =
        runTest {
            val program = createProgram(draft).getOrThrow()

            assertEquals(program.id.value, saved.captured.id)

            assertEquals("My studySet", saved.captured.title["en"])
        }

    @Test
    fun `the goal is the study set the program teaches`() =
        runTest {
            val config = createProgram(draft).getOrThrow().config

            assertEquals(ScopeSourceType.STUDY_SET, config.scope.include.single().type)
            assertEquals(STUDY_SET, config.goals.first { it.type == TargetType.VOCABULARY }.target)
        }

    @Test
    fun `the queue is the day, turn for turn and in order`() =
        runTest {
            val plan = createProgram(draft).getOrThrow().config.dailyPlan

            assertEquals(listOf("word_match", "dictation", "word_match"), plan.queue)

            assertEquals(3, plan.trainingsADay)
            assertEquals(15, plan.newWords)
            assertEquals(30, plan.reviewWords)
        }

    @Test
    fun `both activities draw on the queue, so reviews have somewhere to run`() =
        runTest {
            val activities = createProgram(draft).getOrThrow().config.dailyPlan.activities

            assertEquals(setOf(ActivityType.LEARN, ActivityType.REVIEW), activities.map { it.type }.toSet())

            activities.forEach { assertEquals(draft.trainings.distinct(), it.trainings) }
        }

    @Test
    fun `a day cannot ask for more words than the study set holds`() =
        runTest {
            val plan = createProgram(
                draft.copy(newWordsPerDay = STUDY_SET * 2, reviewWordsPerDay = STUDY_SET * 2),
            ).getOrThrow().config.dailyPlan

            assertEquals(STUDY_SET, plan.newWords)
            assertEquals(STUDY_SET, plan.reviewWords)
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
            coEvery { vocabulary.studySetWordIds() } returns emptyList()

            assertEquals(ProgramDraftProblem.EMPTY_STUDY_SET, problemOf(draft))
            assertFalse(saved.isCaptured)
        }

    private suspend fun problemOf(draft: ProgramDraft): ProgramDraftProblem =
        (createProgram(draft).exceptionOrNull() as ProgramDraftException).problem
}

private class FixedClock(private val nowMillis: Long) : Clock {
    override fun nowEpochMillis(): Long = nowMillis

    override fun todayEpochDay(): Long = nowMillis / 86_400_000L
}
