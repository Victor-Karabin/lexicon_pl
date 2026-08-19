package com.lexicon.presentation.dictation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.lexicon.android.speech.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.dictation.DictationSessionResponse
import com.lexicon.interactors.dictation.DictationStepResponse
import com.lexicon.interactors.dictation.StartDictationSessionRequest
import com.lexicon.interactors.dictation.StartDictationSessionUseCase
import com.lexicon.interactors.dictation.SubmitDictationAnswerResponse
import com.lexicon.interactors.dictation.SubmitDictationAnswerUseCase
import com.lexicon.interactors.training.StepOutcome
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.TRAINING_WORDS_ARG
import com.lexicon.presentation.common.asTrainingWordsArgument
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
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
    private val lastSessionResultsHolder = LastSessionResultsHolder()

    private val translations = mapOf("kot" to "cat", "pies" to "dog")

    private fun session(vararg words: String) =
        DictationSessionResponse(
            sessionId = "session-1",
            steps =
                words.mapIndexed { index, word ->
                    DictationStepResponse(
                        stepIndex = index,
                        vocabularyItemId = index.toLong(),
                        expectedText = word,
                        translationText = translations.getValue(word),
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
        DictationViewModel(
            SavedStateHandle(mapOf(TRAINING_WORDS_ARG to vocabularyIds.asTrainingWordsArgument())),
            startUseCase,
            submitUseCase,
            speechSynthesizer,
            dispatchers,
            lastSessionResultsHolder,
        )

    @Test
    fun `a lesson's word list reaches the session request`() =
        runTest {
            val request = slot<StartDictationSessionRequest>()
            coEvery { startUseCase(capture(request)) } returns session("kot", "pies")

            viewModel(vocabularyIds = listOf(7L, 11L, 13L))
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(listOf(7L, 11L, 13L), request.captured.vocabularyIds)
        }

    @Test
    fun `an unscoped training draws from the whole study set`() =
        runTest {
            val request = slot<StartDictationSessionRequest>()
            coEvery { startUseCase(capture(request)) } returns session("kot", "pies")

            viewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(emptyList<Long>(), request.captured.vocabularyIds)
        }

    @Test
    fun `loads the first step and speaks it on init`() =
        runTest {
            coEvery { startUseCase(any()) } returns session("kot", "pies")

            val viewModel = viewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value as DictationUiState.Loaded
            assertEquals(0, state.stepIndex)
            assertEquals(2, state.totalSteps)
            coVerify { speechSynthesizer.speak("kot") }
        }

    @Test
    fun `correct answer marks Correct, auto-advances, then reports session complete`() =
        runTest {
            coEvery { startUseCase(any()) } returns session("kot")
            coEvery { submitUseCase(any()) } returns SubmitDictationAnswerResponse(StepOutcome.CORRECT, "kot")

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
            assertTrue((viewModel.uiState.value as DictationUiState.Loaded).isSessionComplete)
        }

    @Test
    fun `incorrect answer reveals the expected text and waits for Next`() =
        runTest {
            coEvery { startUseCase(any()) } returns session("kot", "pies")
            coEvery { submitUseCase(any()) } returns SubmitDictationAnswerResponse(StepOutcome.INCORRECT, "kot")

            val viewModel = viewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onAnswerChanged("wrong")
            viewModel.onCheck()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value as DictationUiState.Loaded
            assertEquals(AnswerState.Incorrect("kot"), state.answerState)
            assertEquals("kot", state.revealedAnswer)
            assertTrue(state.awaitingNext)
            assertEquals(0, state.stepIndex)
        }

    @Test
    fun `skip records Skipped, reveals the expected text, then auto-advances`() =
        runTest {
            coEvery { startUseCase(any()) } returns session("kot")
            coEvery { submitUseCase(any()) } returns SubmitDictationAnswerResponse(StepOutcome.SKIPPED, "kot")

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
            assertTrue((viewModel.uiState.value as DictationUiState.Loaded).isSessionComplete)
        }

    @Test
    fun `tip reveals the base-language translation, not the expected answer, without submitting`() =
        runTest {
            coEvery { startUseCase(any()) } returns session("kot")

            val viewModel = viewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onTipRequested()

            val state = viewModel.uiState.value as DictationUiState.Loaded
            assertTrue(state.tipUsed)
            assertEquals("cat", state.tipTranslation)
            assertEquals(null, state.revealedAnswer)
            assertEquals(AnswerState.Unanswered, state.answerState)
            coVerify(exactly = 0) { submitUseCase(any()) }
        }

    @Test
    fun `rapid double-tap on Check only submits once`() =
        runTest {
            coEvery { startUseCase(any()) } returns session("kot")
            coEvery { submitUseCase(any()) } returns SubmitDictationAnswerResponse(StepOutcome.INCORRECT, "kot")

            val viewModel = viewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onAnswerChanged("kot")
            viewModel.onCheck()
            viewModel.onCheck()
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) { submitUseCase(any()) }
        }

    @Test
    fun `rapid double-tap on Next only advances once`() =
        runTest {
            coEvery { startUseCase(any()) } returns session("kot", "pies")
            coEvery { submitUseCase(any()) } returns SubmitDictationAnswerResponse(StepOutcome.INCORRECT, "kot")

            val viewModel = viewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.onAnswerChanged("wrong")
            viewModel.onCheck()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onNext()
            viewModel.onNext()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, (viewModel.uiState.value as DictationUiState.Loaded).stepIndex)
        }

    @Test
    fun `session completion stashes a per-word result for the Results screen`() =
        runTest {
            coEvery { startUseCase(any()) } returns session("kot")
            coEvery { submitUseCase(any()) } returns SubmitDictationAnswerResponse(StepOutcome.CORRECT, "kot")

            val viewModel = viewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onAnswerChanged("kot")
            viewModel.onCheck()
            testDispatcher.scheduler.advanceUntilIdle()

            val entry = lastSessionResultsHolder.wordResults.single()
            assertEquals("kot", entry.word)
            assertEquals("cat", entry.translation)
            assertEquals(AnswerState.Correct, entry.outcome)
        }
}
