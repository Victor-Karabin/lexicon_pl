package com.lexicon.presentation.conjugation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.conjugation.ConjugationStep
import com.lexicon.interactors.conjugation.ConjugationTable
import com.lexicon.interactors.conjugation.ConjugationVariant
import com.lexicon.interactors.conjugation.GrammaticalPerson
import com.lexicon.interactors.conjugation.LoadConjugationCoursesUseCase
import com.lexicon.interactors.conjugation.NextConjugationTableUseCase
import com.lexicon.interactors.conjugation.SubmitConjugationAnswerResponse
import com.lexicon.interactors.conjugation.SubmitConjugationAnswerUseCase
import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.settings.GetStepCountUseCase
import com.lexicon.interactors.training.RecordStudyActivityUseCase
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.SessionNavigationEvent
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConjugationViewModelSessionTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers =
        object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

    private val table = ConjugationTable(
        infinitive = "czytać",
        steps = persistentListOf(ConjugationStep(ConjugationVariant("czytać", GrammaticalPerson.JA), persistentListOf("czytam"))),
    )

    private val nextQuestion: NextConjugationTableUseCase = mockk { coEvery { this@mockk(any(), any(), any()) } returns table }
    private val submitAnswer: SubmitConjugationAnswerUseCase = mockk {
        coEvery { this@mockk(any()) } returns SubmitConjugationAnswerResponse(mapOf(GrammaticalPerson.JA to AnswerVerdict.CORRECT))
    }
    private val getStepCount: GetStepCountUseCase = mockk { coEvery { this@mockk() } returns STEPS }

    @Before
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(recordStudy: RecordStudyActivityUseCase = mockk(relaxed = true)) =
        ConjugationViewModel(
            savedStateHandle = SavedStateHandle(mapOf(CONJUGATION_COURSE_ARG to "course-1")),
            nextQuestion = nextQuestion,
            submitAnswer = submitAnswer,
            loadImageChoices = mockk(),
            chooseImage = mockk(),
            loadCourses = mockk<LoadConjugationCoursesUseCase>(),
            speechSynthesizer = mockk(relaxed = true),
            lastSessionResultsHolder = LastSessionResultsHolder(),
            recordStudy = recordStudy,
            getStepCount = getStepCount,
            dispatchers = dispatchers,
        )

    private fun TestScope.answer(viewModel: ConjugationViewModel) {
        viewModel.onAnswerChanged(GrammaticalPerson.JA, "czytam")
        viewModel.onCheck()
        advanceUntilIdle()
    }

    @Test
    fun `the session is as long as the Steps per session setting`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals(STEPS, viewModel.uiState.value.totalSteps)
        }

    @Test
    fun `the session ends after as many tables as the setting says`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.navigationEvents.test {
                repeat(STEPS - 1) {
                    answer(viewModel)
                    viewModel.onNext()
                    advanceUntilIdle()
                    expectNoEvents()
                }

                answer(viewModel)
                viewModel.onNext()
                advanceUntilIdle()

                awaitItem() as SessionNavigationEvent.SessionComplete
            }
        }

    @Test
    fun `pressing Check twice before the first check returns submits the answers once`() =
        runTest(testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            coEvery { submitAnswer(any()) } coAnswers {
                gate.await()
                SubmitConjugationAnswerResponse(mapOf(GrammaticalPerson.JA to AnswerVerdict.CORRECT))
            }
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onAnswerChanged(GrammaticalPerson.JA, "czytam")

            viewModel.onCheck()
            viewModel.onCheck()
            advanceUntilIdle()
            gate.complete(Unit)
            advanceUntilIdle()

            coVerify(exactly = 1) { submitAnswer(any()) }
        }

    @Test
    fun `the next table is picked knowing which verbs this session has already asked`() =
        runTest(testDispatcher) {
            val seen = mutableListOf<Set<String>>()
            coEvery { nextQuestion(any(), any(), any()) } answers {
                seen += secondArg<Set<String>>().toSet()
                table
            }
            val viewModel = viewModel()
            advanceUntilIdle()
            answer(viewModel)

            viewModel.onNext()
            advanceUntilIdle()

            assertEquals(listOf(emptySet(), setOf("czytać")), seen)
        }

    @Test
    fun `pressing Next twice moves on by one table only`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()
            answer(viewModel)

            viewModel.onNext()
            viewModel.onNext()
            advanceUntilIdle()

            assertEquals(1, viewModel.uiState.value.stepIndex)
            coVerify(exactly = 2) { nextQuestion(any(), any(), any()) }
        }

    @Test
    fun `pressing Next twice marks study activity once`() =
        runTest(testDispatcher) {
            val recordStudy = mockk<RecordStudyActivityUseCase>(relaxed = true)
            val viewModel = viewModel(recordStudy)
            advanceUntilIdle()
            answer(viewModel)
            clearMocks(recordStudy, answers = false)

            viewModel.onNext()
            viewModel.onNext()
            advanceUntilIdle()

            coVerify(exactly = 1) { recordStudy() }
        }

    @Test
    fun `Next does nothing while the table is unanswered`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()
            answer(viewModel)
            viewModel.onNext()
            advanceUntilIdle()

            viewModel.onNext()
            advanceUntilIdle()

            assertEquals(1, viewModel.uiState.value.stepIndex)
            coVerify(exactly = 2) { nextQuestion(any(), any(), any()) }
        }

    private companion object {
        const val STEPS = 3
    }
}
