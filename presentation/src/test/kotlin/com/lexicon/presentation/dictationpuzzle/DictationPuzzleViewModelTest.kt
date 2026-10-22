package com.lexicon.presentation.dictationpuzzle

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.dictationpuzzle.DictationPuzzleSessionResponse
import com.lexicon.interactors.dictationpuzzle.DictationPuzzleStepResponse
import com.lexicon.interactors.dictationpuzzle.StartDictationPuzzleSessionUseCase
import com.lexicon.interactors.dictationpuzzle.SubmitDictationPuzzleAnswerRequest
import com.lexicon.interactors.dictationpuzzle.SubmitDictationPuzzleAnswerResponse
import com.lexicon.interactors.dictationpuzzle.SubmitDictationPuzzleAnswerUseCase
import com.lexicon.model.training.StepOutcome
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.puzzle.PuzzleUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DictationPuzzleViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers =
        object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

    private val start: StartDictationPuzzleSessionUseCase = mockk()
    private val submit: SubmitDictationPuzzleAnswerUseCase = mockk()
    private val holder = LastSessionResultsHolder()

    private fun step(
        index: Int,
        text: String,
        translation: String,
    ) = DictationPuzzleStepResponse(index, vocabularyItemId = index + 1L, expectedText = text, translationText = translation)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { start(any()) } returns
            DictationPuzzleSessionResponse("session-1", listOf(step(0, "kot", "cat"), step(1, "pies", "dog")))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(): DictationPuzzleViewModel =
        DictationPuzzleViewModel(SavedStateHandle(), start, submit, mockk(relaxed = true), dispatchers, holder)
            .also { advanceUntilIdle() }

    private fun DictationPuzzleViewModel.loaded() = uiState.value as PuzzleUiState.Loaded

    private fun DictationPuzzleViewModel.placeAllInOrder() {
        loaded().stepTiles.sortedBy { it.id }.forEach { onTileSelected(it) }
    }

    @Test
    fun `the first word opens as a shuffled set of its letters with nothing placed`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()

            val state = viewModel.loaded()
            assertEquals(0, state.stepIndex)
            assertEquals(2, state.totalSteps)
            assertEquals("kot".toList().sorted(), state.stepTiles.map { it.char }.sorted())
            assertTrue(state.placedTiles.isEmpty())
            assertEquals(AnswerState.Unanswered, state.answerState)
            assertNull(state.tipTranslation)
            assertNull(state.imageUrl)
            assertEquals("", state.clueText)
            assertFalse(state.tipUsed)
            assertFalse(state.isSessionComplete)
            assertFalse(state.isSubmitting)
        }

    @Test
    fun `a session with no words is unavailable`() =
        runTest(testDispatcher) {
            coEvery { start(any()) } returns DictationPuzzleSessionResponse("session-1", emptyList())

            val viewModel = viewModel()

            assertEquals(PuzzleUiState.Unavailable, viewModel.uiState.value)
        }

    @Test
    fun `the tip shows the translation once`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()

            viewModel.onTipRequested()

            assertEquals("cat", viewModel.loaded().tipTranslation)
            assertTrue(viewModel.loaded().tipUsed)
            assertFalse(viewModel.loaded().canUseTip)
        }

    @Test
    fun `a correct answer is submitted with the word and the tip flag and shown as correct`() =
        runTest(testDispatcher) {
            val request = slot<SubmitDictationPuzzleAnswerRequest>()
            coEvery { submit(capture(request)) } returns SubmitDictationPuzzleAnswerResponse(StepOutcome.CORRECT, "kot")
            val viewModel = viewModel()
            viewModel.onTipRequested()
            viewModel.placeAllInOrder()

            viewModel.onCheck()
            testScheduler.runCurrent()

            assertEquals(
                SubmitDictationPuzzleAnswerRequest(
                    sessionId = "session-1",
                    stepIndex = 0,
                    vocabularyItemId = 1L,
                    expectedText = "kot",
                    submittedText = "kot",
                    tipUsed = true,
                    skipped = false,
                ),
                request.captured,
            )
            assertEquals(AnswerState.Correct, viewModel.loaded().answerState)
        }

    @Test
    fun `answering every word ends the session with the tally and the word results`() =
        runTest(testDispatcher) {
            coEvery { submit(any()) } returns SubmitDictationPuzzleAnswerResponse(StepOutcome.CORRECT, "x")
            val viewModel = viewModel()

            viewModel.navigationEvents.test {
                viewModel.onTipRequested()
                viewModel.placeAllInOrder()
                viewModel.onCheck()
                advanceUntilIdle()
                viewModel.placeAllInOrder()
                viewModel.onCheck()
                advanceUntilIdle()

                assertEquals(SessionNavigationEvent.SessionComplete(correct = 2, incorrect = 0, skipped = 0, tipsUsed = 1), awaitItem())
            }
            assertTrue(viewModel.loaded().isSessionComplete)
            assertEquals(listOf("kot" to "cat", "pies" to "dog"), holder.wordResults.map { it.word to it.translation })
            assertEquals(listOf(true, false), holder.wordResults.map { it.tipUsed })
            coVerify(exactly = 2) { submit(any()) }
        }
}
