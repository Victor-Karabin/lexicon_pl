package com.lexicon.presentation.declension

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.declension.DeclensionColumn
import com.lexicon.interactors.declension.DeclensionCourse
import com.lexicon.interactors.declension.DeclensionCourseProgress
import com.lexicon.interactors.declension.DeclensionStep
import com.lexicon.interactors.declension.DeclensionTable
import com.lexicon.interactors.declension.DeclensionVariant
import com.lexicon.interactors.declension.DeclensionVariantProgress
import com.lexicon.interactors.declension.LoadDeclensionCoursesUseCase
import com.lexicon.interactors.declension.NextDeclensionTableUseCase
import com.lexicon.interactors.declension.SubmitDeclensionAnswerResponse
import com.lexicon.interactors.declension.SubmitDeclensionAnswerUseCase
import com.lexicon.interactors.settings.GetStepCountUseCase
import com.lexicon.interactors.training.TABLE_DRILL_MASTERY_STREAK
import com.lexicon.model.vocabulary.GrammaticalCase
import com.lexicon.model.vocabulary.PartOfSpeech
import com.lexicon.presentation.R
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.TextResource
import com.lexicon.presentation.common.WordResultEntry
import com.lexicon.presentation.common.labelId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeclensionViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers =
        object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

    private val table = DeclensionTable(
        word = "kot",
        translation = "cat",
        partOfSpeech = PartOfSpeech.NOUN,
        gender = null,
        column = DeclensionColumn.SINGULAR,
        steps = persistentListOf(
            DeclensionStep(DeclensionVariant("kot", DeclensionColumn.SINGULAR, GrammaticalCase.GENITIVE), persistentListOf("kota")),
        ),
    )

    private val twoCaseTable = table.copy(
        steps = persistentListOf(
            table.steps.single(),
            DeclensionStep(DeclensionVariant("kot", DeclensionColumn.SINGULAR, GrammaticalCase.DATIVE), persistentListOf("kotu")),
        ),
    )

    private val nextQuestion: NextDeclensionTableUseCase = mockk { coEvery { this@mockk(any(), any(), any()) } returns table }
    private val submitAnswer: SubmitDeclensionAnswerUseCase = mockk {
        coEvery { this@mockk(any()) } returns SubmitDeclensionAnswerResponse(mapOf(GrammaticalCase.GENITIVE to AnswerVerdict.CORRECT))
    }
    private val loadCourses: LoadDeclensionCoursesUseCase = mockk()
    private val results = LastSessionResultsHolder()
    private val getStepCount: GetStepCountUseCase = mockk { coEvery { this@mockk() } returns STEPS }

    @Before
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        DeclensionViewModel(
            savedStateHandle = SavedStateHandle(),
            nextQuestion = nextQuestion,
            submitAnswer = submitAnswer,
            loadCourses = loadCourses,
            speechSynthesizer = mockk(relaxed = true),
            lastSessionResultsHolder = results,
            recordStudy = mockk(relaxed = true),
            getStepCount = getStepCount,
            dispatchers = dispatchers,
        )

    @Test
    fun `the next table is picked knowing which words this session has already asked`() =
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

            assertEquals(listOf(emptySet(), setOf("kot")), seen)
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

    private fun resultLabel(case: GrammaticalCase) =
        TextResource(
            R.string.declension_result_label,
            listOf("kot", TextResource(DeclensionColumn.SINGULAR.labelId()), TextResource(case.labelId())),
        )

    private fun masteredCourse() =
        DeclensionCourse(
            id = "course",
            words = persistentListOf("kot"),
            progress = DeclensionCourseProgress(
                persistentListOf(DeclensionVariantProgress(table.steps.single().variant, streak = TABLE_DRILL_MASTERY_STREAK)),
            ),
        )

    private fun TestScope.answer(viewModel: DeclensionViewModel) {
        viewModel.onAnswerChanged(GrammaticalCase.GENITIVE, "kota")
        viewModel.onCheck()
        advanceUntilIdle()
    }

    private fun TestScope.answerTwoCases(viewModel: DeclensionViewModel) {
        viewModel.onAnswerChanged(GrammaticalCase.GENITIVE, "kota")
        viewModel.onAnswerChanged(GrammaticalCase.DATIVE, "kotku")
        viewModel.onCheck()
        advanceUntilIdle()
    }

    @Test
    fun `a checked table keeps the verdicts and shows the answer as wrong when one case is wrong`() =
        runTest(testDispatcher) {
            coEvery { nextQuestion(any(), any(), any()) } returns twoCaseTable
            val verdicts = mapOf(GrammaticalCase.GENITIVE to AnswerVerdict.CORRECT, GrammaticalCase.DATIVE to AnswerVerdict.WRONG)
            coEvery { submitAnswer(any()) } returns SubmitDeclensionAnswerResponse(verdicts)
            val viewModel = viewModel()
            advanceUntilIdle()

            answerTwoCases(viewModel)

            assertEquals(verdicts.toImmutableMap(), viewModel.uiState.value.verdicts)
            assertEquals(AnswerState.Incorrect(""), viewModel.uiState.value.answerState)
            assertFalse(viewModel.uiState.value.isChecking)
        }

    @Test
    fun `a table with every case right is shown as correct`() =
        runTest(testDispatcher) {
            coEvery { submitAnswer(any()) } returns SubmitDeclensionAnswerResponse(mapOf(GrammaticalCase.GENITIVE to AnswerVerdict.CORRECT))
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onAnswerChanged(GrammaticalCase.GENITIVE, "kota")
            viewModel.onCheck()
            advanceUntilIdle()

            assertEquals(AnswerState.Correct, viewModel.uiState.value.answerState)
        }

    @Test
    fun `the result lists every case with what was typed and the form it should have been`() =
        runTest(testDispatcher) {
            coEvery { nextQuestion(any(), any(), any()) } returns twoCaseTable
            coEvery { submitAnswer(any()) } returns
                SubmitDeclensionAnswerResponse(
                    mapOf(GrammaticalCase.GENITIVE to AnswerVerdict.CORRECT, GrammaticalCase.DATIVE to AnswerVerdict.WRONG),
                )
            coEvery { getStepCount() } returns 1
            val viewModel = viewModel()
            advanceUntilIdle()
            answerTwoCases(viewModel)

            viewModel.navigationEvents.test {
                viewModel.onNext()
                advanceUntilIdle()

                assertEquals(SessionNavigationEvent.SessionComplete(correct = 1, incorrect = 1, skipped = 0, tipsUsed = 0), awaitItem())
            }
            assertEquals(
                listOf(
                    WordResultEntry(
                        word = "kota",
                        translation = "kot",
                        outcome = AnswerState.Correct,
                        typed = "kota",
                        translationText = resultLabel(GrammaticalCase.GENITIVE),
                    ),
                    WordResultEntry(
                        word = "kotu",
                        translation = "kot",
                        outcome = AnswerState.Incorrect("kotu"),
                        typed = "kotku",
                        translationText = resultLabel(GrammaticalCase.DATIVE),
                    ),
                ),
                results.wordResults,
            )
        }

    @Test
    fun `the results are in place by the time the event arrives`() =
        runTest(testDispatcher) {
            coEvery { getStepCount() } returns 1
            val viewModel = viewModel()
            advanceUntilIdle()
            answer(viewModel)
            var seen: List<WordResultEntry>? = null
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.navigationEvents.collect { seen = results.wordResults }
            }

            viewModel.onNext()
            advanceUntilIdle()

            assertEquals(listOf("kota"), seen?.map { it.word })
        }

    @Test
    fun `with nothing to practise and the course unfinished the screen shows none`() =
        runTest(testDispatcher) {
            coEvery { nextQuestion(any(), any(), any()) } returns null
            coEvery { loadCourses() } returns persistentListOf()

            val viewModel = viewModel()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.table)
            assertFalse(viewModel.uiState.value.isComplete)
        }

    @Test
    fun `with nothing left to practise in a finished course the screen says it is complete`() =
        runTest(testDispatcher) {
            coEvery { nextQuestion(any(), any(), any()) } returns null
            coEvery { loadCourses() } returns persistentListOf(masteredCourse())

            val viewModel = viewModel()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.table)
            assertTrue(viewModel.uiState.value.isComplete)
        }

    @Test
    fun `a first table clears the loading flag without asking about the course`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(table, viewModel.uiState.value.table)
            assertFalse(viewModel.uiState.value.isComplete)
            assertEquals(0, viewModel.uiState.value.stepIndex)
        }

    @Test
    fun `next moves to a fresh table with the answers cleared`() =
        runTest(testDispatcher) {
            coEvery { submitAnswer(any()) } returns SubmitDeclensionAnswerResponse(mapOf(GrammaticalCase.GENITIVE to AnswerVerdict.CORRECT))
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onAnswerChanged(GrammaticalCase.GENITIVE, "kota")
            viewModel.onCheck()
            advanceUntilIdle()

            viewModel.onNext()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(1, state.stepIndex)
            assertTrue(state.answers.isEmpty())
            assertTrue(state.verdicts.isEmpty())
            assertEquals(AnswerState.Unanswered, state.answerState)
        }

    @Test
    fun `a fast double next moves one step`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()
            answer(viewModel)

            viewModel.onNext()
            viewModel.onNext()
            advanceUntilIdle()

            assertEquals(1, viewModel.uiState.value.stepIndex)
        }

    @Test
    fun `next does nothing while the table is unanswered`() =
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
