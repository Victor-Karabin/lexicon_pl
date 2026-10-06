package com.lexicon.presentation.memorycards

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.memorycards.MemoryCardsPairResponse
import com.lexicon.interactors.memorycards.MemoryCardsSessionResponse
import com.lexicon.interactors.memorycards.MemoryCardsStepResponse
import com.lexicon.interactors.memorycards.StartMemoryCardsSessionUseCase
import com.lexicon.interactors.memorycards.SubmitMemoryCardsStepResultResponse
import com.lexicon.interactors.memorycards.SubmitMemoryCardsStepResultUseCase
import com.lexicon.model.training.StepOutcome
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.TRAINING_WORDS_ARG
import com.lexicon.presentation.common.WordResultEntry
import com.lexicon.presentation.common.asTrainingWordsArgument
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MemoryCardsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers =
        object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

    private val startUseCase: StartMemoryCardsSessionUseCase = mockk()
    private val submitUseCase: SubmitMemoryCardsStepResultUseCase = mockk()
    private val lastSessionResultsHolder = LastSessionResultsHolder()

    private fun session() =
        MemoryCardsSessionResponse(
            sessionId = "session-1",
            steps = listOf(
                MemoryCardsStepResponse(
                    stepIndex = 0,
                    pairs = listOf(
                        MemoryCardsPairResponse(
                            vocabularyItemId = 1L,
                            imageUrl = null,
                            imageFallbackText = "kot",
                            text = "kot",
                        ),
                    ),
                ),
            ),
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(vocabularyIds: List<Long> = emptyList()) =
        MemoryCardsViewModel(
            SavedStateHandle(mapOf(TRAINING_WORDS_ARG to vocabularyIds.asTrainingWordsArgument())),
            startUseCase,
            submitUseCase,
            dispatchers,
            lastSessionResultsHolder,
        )

    @Test
    fun `session completion clears a previous training's stale word list rather than leaving it`() =
        runTest {
            lastSessionResultsHolder.wordResults = listOf(WordResultEntry("kot", "cat", AnswerState.Correct))
            coEvery { startUseCase(any()) } returns session()
            coEvery { submitUseCase(any()) } returns SubmitMemoryCardsStepResultResponse(StepOutcome.SKIPPED)

            val viewModel = viewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onSkip()
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(lastSessionResultsHolder.wordResults.isEmpty())
        }

    @Test
    fun `skip records Skipped and auto-advances without a manual Next`() =
        runTest {
            coEvery { startUseCase(any()) } returns session()
            coEvery { submitUseCase(any()) } returns SubmitMemoryCardsStepResultResponse(StepOutcome.SKIPPED)

            val viewModel = viewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.navigationEvents.test {
                viewModel.onSkip()
                testDispatcher.scheduler.advanceUntilIdle()

                val event = awaitItem() as SessionNavigationEvent.SessionComplete
                assertEquals(0, event.correct)
                assertEquals(0, event.incorrect)
                assertEquals(1, event.skipped)
            }
        }

    @Test
    fun `skip is refused while the matched board is being submitted, so the next board is not jumped over`() =
        runTest {
            val oneStep = session().steps.single()
            coEvery { startUseCase(any()) } returns
                session().copy(steps = listOf(oneStep, oneStep.copy(stepIndex = 1)))
            coEvery { submitUseCase(any()) } returns SubmitMemoryCardsStepResultResponse(StepOutcome.CORRECT)

            val viewModel = viewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onCardSelected(0)
            viewModel.onCardSelected(1)
            assertFalse((viewModel.uiState.value as MemoryCardsUiState.Loaded).canSkip)
            viewModel.onSkip()
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) { submitUseCase(any()) }
            val state = viewModel.uiState.value as MemoryCardsUiState.Loaded
            assertEquals(1, state.stepIndex)
            assertTrue(state.canSkip)
        }
}
