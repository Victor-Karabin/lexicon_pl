package com.lexicon.presentation.puzzle

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.puzzle.PuzzleSessionResponse
import com.lexicon.interactors.puzzle.PuzzleStepResponse
import com.lexicon.interactors.puzzle.StartPuzzleSessionUseCase
import com.lexicon.interactors.puzzle.SubmitPuzzleAnswerRequest
import com.lexicon.interactors.puzzle.SubmitPuzzleAnswerResponse
import com.lexicon.interactors.puzzle.SubmitPuzzleAnswerUseCase
import com.lexicon.model.training.StepOutcome
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.SessionNavigationEvent
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
class PuzzleViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers =
        object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

    private val start: StartPuzzleSessionUseCase = mockk()
    private val submit: SubmitPuzzleAnswerUseCase = mockk()
    private val holder = LastSessionResultsHolder()

    private fun step(
        index: Int,
        text: String,
        clue: String,
        imageUrl: String?,
    ) = PuzzleStepResponse(index, vocabularyItemId = index + 1L, expectedText = text, imageUrl = imageUrl, clueText = clue)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { start(any()) } returns
            PuzzleSessionResponse(
                "session-1",
                listOf(step(0, "kot", "cat", "https://example.com/cat.jpg"), step(1, "pies", "dog", null)),
            )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(): PuzzleViewModel =
        PuzzleViewModel(SavedStateHandle(), start, submit, dispatchers, holder).also { advanceUntilIdle() }

    private fun PuzzleViewModel.loaded() = uiState.value as PuzzleUiState.Loaded

    private fun PuzzleViewModel.placeAllInOrder() {
        loaded().stepTiles.sortedBy { it.id }.forEach { onTileSelected(it) }
    }

    @Test
    fun `the first word opens with its picture, its clue and a shuffled set of its letters`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()

            val state = viewModel.loaded()
            assertEquals(0, state.stepIndex)
            assertEquals(2, state.totalSteps)
            assertEquals("https://example.com/cat.jpg", state.imageUrl)
            assertEquals("cat", state.clueText)
            assertEquals("kot".toList().sorted(), state.stepTiles.map { it.char }.sorted())
            assertTrue(state.placedTiles.isEmpty())
            assertEquals(AnswerState.Unanswered, state.answerState)
            assertNull(state.tipTranslation)
            assertFalse(state.tipUsed)
            assertFalse(state.isSessionComplete)
            assertFalse(state.isSubmitting)
        }

    @Test
    fun `a word without a picture opens with no image`() =
        runTest(testDispatcher) {
            coEvery { start(any()) } returns PuzzleSessionResponse("session-1", listOf(step(0, "pies", "dog", null)))

            val viewModel = viewModel()

            assertNull(viewModel.loaded().imageUrl)
            assertEquals("dog", viewModel.loaded().clueText)
        }

    @Test
    fun `a session with no words is unavailable`() =
        runTest(testDispatcher) {
            coEvery { start(any()) } returns PuzzleSessionResponse("session-1", emptyList())

            val viewModel = viewModel()

            assertEquals(PuzzleUiState.Unavailable, viewModel.uiState.value)
        }

    @Test
    fun `the tip shows the clue as the translation once`() =
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
            val request = slot<SubmitPuzzleAnswerRequest>()
            coEvery { submit(capture(request)) } returns SubmitPuzzleAnswerResponse(StepOutcome.CORRECT, "kot")
            val viewModel = viewModel()
            viewModel.onTipRequested()
            viewModel.placeAllInOrder()

            viewModel.onCheck()
            testScheduler.runCurrent()

            assertEquals(
                SubmitPuzzleAnswerRequest(
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
            coEvery { submit(any()) } returns SubmitPuzzleAnswerResponse(StepOutcome.CORRECT, "x")
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
