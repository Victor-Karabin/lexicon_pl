package com.lexicon.presentation.crossword

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.crossword.CrosswordDirection
import com.lexicon.interactors.crossword.CrosswordSessionResponse
import com.lexicon.interactors.crossword.CrosswordWordPlacement
import com.lexicon.interactors.crossword.CrosswordWordResult
import com.lexicon.interactors.crossword.StartCrosswordSessionUseCase
import com.lexicon.interactors.crossword.SubmitCrosswordRequest
import com.lexicon.interactors.crossword.SubmitCrosswordResponse
import com.lexicon.interactors.crossword.SubmitCrosswordUseCase
import com.lexicon.model.training.StepOutcome
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.WordResultEntry
import io.mockk.coEvery
import io.mockk.mockk
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CrosswordViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers =
        object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

    private val start: StartCrosswordSessionUseCase = mockk()
    private val submit: SubmitCrosswordUseCase = mockk()
    private val holder = LastSessionResultsHolder()

    private val placements = listOf(
        CrosswordWordPlacement(1L, "kot", "cat", 0, 0, CrosswordDirection.ACROSS),
        CrosswordWordPlacement(2L, "dom", "house", 2, 0, CrosswordDirection.ACROSS),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { start(any()) } returns CrosswordSessionResponse("session-1", placements, rowCount = 3, colCount = 3)
        coEvery { submit(any()) } answers { gradedLikeTheUseCase(firstArg()) }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun gradedLikeTheUseCase(request: SubmitCrosswordRequest) =
        SubmitCrosswordResponse(
            wordResults = request.words.map {
                val matches = it.expectedText.equals(it.submittedText, ignoreCase = true)
                val outcome = if (matches) StepOutcome.CORRECT else StepOutcome.INCORRECT
                CrosswordWordResult(it.vocabularyItemId, it.expectedText, outcome, it.tipUsed)
            },
            isFullyCorrect = false,
        )

    private fun TestScope.viewModel(): CrosswordViewModel =
        CrosswordViewModel(SavedStateHandle(), start, submit, dispatchers, holder).also { advanceUntilIdle() }

    private fun CrosswordViewModel.loaded() = uiState.value as CrosswordUiState.Loaded

    private fun TestScope.type(
        viewModel: CrosswordViewModel,
        word: CrosswordWordUi,
        text: String,
    ) {
        word.occupiedCells().zip(text.toList()).forEach { (cell, letter) -> viewModel.onLetterEntered(cell, letter.toString()) }
        advanceUntilIdle()
    }

    private fun CrosswordViewModel.word(id: Long) = loaded().words.single { it.vocabularyItemId == id }

    @Test
    fun `every word answered correctly is a correct board and a clean result`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()

            viewModel.navigationEvents.test {
                type(viewModel, viewModel.word(1L), "kot")
                type(viewModel, viewModel.word(2L), "dom")

                assertEquals(SessionNavigationEvent.SessionComplete(correct = 2, incorrect = 0, skipped = 0, tipsUsed = 0), awaitItem())
            }
            assertEquals(AnswerState.Correct, viewModel.loaded().answerState)
            assertFalse(viewModel.loaded().isSubmitting)
            assertFalse(viewModel.loaded().submitFailed)
            assertEquals(null, viewModel.loaded().focusedCell)
            assertEquals(
                listOf(
                    WordResultEntry("kot", "cat", AnswerState.Correct, tipUsed = false),
                    WordResultEntry("dom", "house", AnswerState.Correct, tipUsed = false),
                ),
                holder.wordResults,
            )
        }

    @Test
    fun `a wrong word and a word with revealed letters are told apart in the result`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()

            viewModel.navigationEvents.test {
                viewModel.onTipRequested()
                viewModel.onTipRequested()
                advanceUntilIdle()
                assertEquals(2, viewModel.word(1L).revealedLetterCount)
                type(viewModel, viewModel.word(1L), "kot")
                type(viewModel, viewModel.word(2L), "dxm")

                assertEquals(SessionNavigationEvent.SessionComplete(correct = 1, incorrect = 1, skipped = 0, tipsUsed = 1), awaitItem())
            }
            assertEquals(AnswerState.Incorrect(), viewModel.loaded().answerState)
            assertEquals(
                listOf(
                    WordResultEntry("kot", "cat", AnswerState.Correct, tipUsed = true),
                    WordResultEntry("dom", "house", AnswerState.Incorrect("dom"), tipUsed = false),
                ),
                holder.wordResults,
            )
        }

    @Test
    fun `tips count once per word however many letters were revealed`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()

            viewModel.navigationEvents.test {
                repeat(3) { viewModel.onTipRequested() }
                advanceUntilIdle()
                viewModel.onClueSelected(2L)
                repeat(2) { viewModel.onTipRequested() }
                advanceUntilIdle()
                type(viewModel, viewModel.word(2L), "dom")

                assertEquals(SessionNavigationEvent.SessionComplete(correct = 2, incorrect = 0, skipped = 0, tipsUsed = 2), awaitItem())
            }
        }

    @Test
    fun `a failed submit shows the failure, leaves the board editable and sends nothing on`() =
        runTest(testDispatcher) {
            val earlier = listOf(WordResultEntry("stale", "old", AnswerState.Correct))
            holder.wordResults = earlier
            coEvery { submit(any()) } throws IllegalStateException("offline")
            val viewModel = viewModel()

            viewModel.navigationEvents.test {
                type(viewModel, viewModel.word(1L), "kot")
                type(viewModel, viewModel.word(2L), "dom")

                expectNoEvents()
            }
            assertTrue(viewModel.loaded().submitFailed)
            assertFalse(viewModel.loaded().isSubmitting)
            assertEquals(AnswerState.Unanswered, viewModel.loaded().answerState)
            assertEquals(earlier, holder.wordResults)
        }

    @Test
    fun `the results and the state are in place by the time the event arrives`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()
            var seen: Triple<List<WordResultEntry>, AnswerState, Boolean>? = null
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.navigationEvents.collect {
                    seen = Triple(holder.wordResults, viewModel.loaded().answerState, viewModel.loaded().isSubmitting)
                }
            }

            type(viewModel, viewModel.word(1L), "kot")
            type(viewModel, viewModel.word(2L), "dom")

            val (results, answerState, isSubmitting) = checkNotNull(seen)
            assertEquals(listOf("kot", "dom"), results.map { it.word })
            assertEquals(AnswerState.Correct, answerState)
            assertFalse(isSubmitting)
        }

    @Test
    fun `nothing is sent before every cell is filled`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()

            viewModel.navigationEvents.test {
                type(viewModel, viewModel.word(1L), "kot")

                expectNoEvents()
            }
            assertEquals(AnswerState.Unanswered, viewModel.loaded().answerState)
            assertFalse(viewModel.loaded().isSubmitting)
        }
}
