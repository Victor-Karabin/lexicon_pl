package com.lexicon.presentation.imagetest

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.imagetest.ImageTestSessionResponse
import com.lexicon.interactors.imagetest.ImageTestStepResponse
import com.lexicon.interactors.imagetest.StartImageTestSessionUseCase
import com.lexicon.interactors.imagetest.SubmitImageTestAnswerResponse
import com.lexicon.interactors.imagetest.SubmitImageTestAnswerUseCase
import com.lexicon.model.training.StepOutcome
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.TRAINING_WORDS_ARG
import com.lexicon.presentation.common.asTrainingWordsArgument
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ImageTestViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers =
        object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

    private val startUseCase: StartImageTestSessionUseCase = mockk()
    private val submitUseCase: SubmitImageTestAnswerUseCase = mockk()
    private val lastSessionResultsHolder = LastSessionResultsHolder()

    private fun session(vararg subjects: Pair<String, String>) =
        ImageTestSessionResponse(
            sessionId = "session-1",
            steps = subjects.mapIndexed { index, (word, clue) ->
                ImageTestStepResponse(
                    stepIndex = index,
                    vocabularyItemId = index.toLong(),
                    imageUrl = null,
                    clueText = clue,
                    options = listOf(word),
                    correctOption = word,
                )
            },
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
        ImageTestViewModel(
            SavedStateHandle(mapOf(TRAINING_WORDS_ARG to vocabularyIds.asTrainingWordsArgument())),
            startUseCase,
            submitUseCase,
            dispatchers,
            lastSessionResultsHolder,
        )

    @Test
    fun `session completion stashes a per-word result for the Results screen`() =
        runTest {
            coEvery { startUseCase(any()) } returns session("kot" to "cat")
            coEvery { submitUseCase(any()) } returns SubmitImageTestAnswerResponse(StepOutcome.CORRECT, "kot")

            val viewModel = viewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onOptionSelected("kot")
            viewModel.onCheck()
            testDispatcher.scheduler.advanceUntilIdle()

            val entry = lastSessionResultsHolder.wordResults.single()
            assertEquals("kot", entry.word)
            assertEquals("cat", entry.translation)
            assertEquals(AnswerState.Correct, entry.outcome)
        }

    @Test
    fun `skip records Skipped and auto-advances without a manual Next`() =
        runTest {
            coEvery { startUseCase(any()) } returns session("kot" to "cat")
            coEvery { submitUseCase(any()) } returns SubmitImageTestAnswerResponse(StepOutcome.SKIPPED, "kot")

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
    fun `an incorrect answer waits for a manual Next, unlike Skip`() =
        runTest {
            coEvery { startUseCase(any()) } returns session("kot" to "cat", "pies" to "dog")
            coEvery { submitUseCase(any()) } returns SubmitImageTestAnswerResponse(StepOutcome.INCORRECT, "kot")

            val viewModel = viewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onOptionSelected("wrong")
            viewModel.onCheck()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value as ImageTestUiState.Loaded
            assertTrue(state.awaitingNext)
            assertEquals(0, state.stepIndex)
        }
}
