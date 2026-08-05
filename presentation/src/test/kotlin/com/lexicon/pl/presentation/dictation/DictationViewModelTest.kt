package com.lexicon.pl.presentation.dictation

import app.cash.turbine.test
import com.lexicon.pl.android.SpeechSynthesizer
import com.lexicon.pl.common.DispatcherProvider
import com.lexicon.pl.interactors.dictation.DictationSessionResponse
import com.lexicon.pl.interactors.dictation.DictationStepOutcome
import com.lexicon.pl.interactors.dictation.DictationStepResponse
import com.lexicon.pl.interactors.dictation.StartDictationSessionUseCase
import com.lexicon.pl.interactors.dictation.SubmitDictationAnswerResponse
import com.lexicon.pl.interactors.dictation.SubmitDictationAnswerUseCase
import com.lexicon.pl.presentation.common.AnswerState
import com.lexicon.pl.presentation.common.SessionNavigationEvent
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DictationViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers =
        object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

    private val startUseCase: StartDictationSessionUseCase = mockk()
    private val submitUseCase: SubmitDictationAnswerUseCase = mockk()
    private val speechSynthesizer: SpeechSynthesizer = mockk(relaxed = true)

    private fun session(vararg words: String) =
        DictationSessionResponse(
            sessionId = "session-1",
            steps =
                words.mapIndexed { index, word ->
                    DictationStepResponse(stepIndex = index, vocabularyItemId = index.toLong(), expectedText = word)
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

    private fun viewModel() = DictationViewModel(startUseCase, submitUseCase, speechSynthesizer, dispatchers)

    @Test
    fun `loads the first step and speaks it on init`() =
        runTest {
            coEvery { startUseCase(any()) } returns session("kot", "pies")

            val viewModel = viewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(false, state.isLoading)
            assertEquals(0, state.stepIndex)
            assertEquals(2, state.totalSteps)
            coVerify { speechSynthesizer.speak("kot") }
        }

    @Test
    fun `correct answer marks Correct, auto-advances, then reports session complete`() =
        runTest {
            coEvery { startUseCase(any()) } returns session("kot")
            coEvery { submitUseCase(any()) } returns SubmitDictationAnswerResponse(DictationStepOutcome.CORRECT, "kot")

            val viewModel = viewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.navigationEvents.test {
                viewModel.onAnswerChanged("kot")
                viewModel.onCheck()
                testDispatcher.scheduler.advanceUntilIdle()

                val event = awaitItem() as SessionNavigationEvent.SessionComplete
                assertEquals(1, event.correct)
                assertEquals(0, event.incorrect)
                assertEquals(0, event.skipped)
            }
            assertTrue(viewModel.uiState.value.isSessionComplete)
        }

    @Test
    fun `incorrect answer reveals the expected text and waits for Next`() =
        runTest {
            coEvery { startUseCase(any()) } returns session("kot", "pies")
            coEvery { submitUseCase(any()) } returns SubmitDictationAnswerResponse(DictationStepOutcome.INCORRECT, "kot")

            val viewModel = viewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onAnswerChanged("wrong")
            viewModel.onCheck()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(AnswerState.INCORRECT, state.answerState)
            assertEquals("kot", state.revealedAnswer)
            assertTrue(state.awaitingNext)
            // Auto-advance only happens for Correct; Incorrect must wait for an explicit Next.
            assertEquals(0, state.stepIndex)
        }

    @Test
    fun `skip records Skipped and reveals the expected text`() =
        runTest {
            coEvery { startUseCase(any()) } returns session("kot")
            coEvery { submitUseCase(any()) } returns SubmitDictationAnswerResponse(DictationStepOutcome.SKIPPED, "kot")

            val viewModel = viewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onSkip()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(AnswerState.SKIPPED, state.answerState)
            assertEquals("kot", state.revealedAnswer)
        }

    @Test
    fun `tip reveals the answer and marks tipUsed without submitting`() =
        runTest {
            coEvery { startUseCase(any()) } returns session("kot")

            val viewModel = viewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onTipRequested()

            val state = viewModel.uiState.value
            assertTrue(state.tipUsed)
            assertEquals("kot", state.revealedAnswer)
            assertEquals(AnswerState.UNANSWERED, state.answerState)
            coVerify(exactly = 0) { submitUseCase(any()) }
        }
}
