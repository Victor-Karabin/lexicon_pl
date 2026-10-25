package com.lexicon.presentation.trueorfalse

import androidx.lifecycle.SavedStateHandle
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.trueorfalse.StartTrueOrFalseSessionUseCase
import com.lexicon.interactors.trueorfalse.SubmitTrueOrFalseAnswerResponse
import com.lexicon.interactors.trueorfalse.SubmitTrueOrFalseAnswerUseCase
import com.lexicon.interactors.trueorfalse.TrueOrFalseSessionResponse
import com.lexicon.interactors.trueorfalse.TrueOrFalseStepResponse
import com.lexicon.model.training.StepOutcome
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.SessionNavigationEvent
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private const val JUST_BEFORE_TIME_IS_UP_MS = TRUE_OR_FALSE_TIME_LIMIT_SECONDS * 1_000L - 500L
private const val PAST_THE_TIME_LIMIT_MS = 1_000L

@OptIn(ExperimentalCoroutinesApi::class)
class TrueOrFalseViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private val steps = listOf(
        TrueOrFalseStepResponse(0, 1L, "kot", "cat", isDisplayedTranslationCorrect = true),
        TrueOrFalseStepResponse(1, 2L, "pies", "dog", isDisplayedTranslationCorrect = true),
    )

    private val start: StartTrueOrFalseSessionUseCase = mockk {
        coEvery { this@mockk(any()) } returns TrueOrFalseSessionResponse("session", steps)
    }
    private val submit: SubmitTrueOrFalseAnswerUseCase = mockk()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        TrueOrFalseViewModel(
            savedStateHandle = SavedStateHandle(),
            startSessionUseCase = start,
            submitAnswerUseCase = submit,
            dispatchers = object : DispatcherProvider {
                override val main: CoroutineDispatcher = dispatcher
                override val io: CoroutineDispatcher = dispatcher
                override val default: CoroutineDispatcher = dispatcher
            },
            lastSessionResultsHolder = LastSessionResultsHolder(),
        )

    private fun TestScope.completions(viewModel: TrueOrFalseViewModel): List<SessionNavigationEvent> {
        val events = mutableListOf<SessionNavigationEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.navigationEvents.collect { events += it } }
        return events
    }

    @Test
    fun `an answer tapped before the time runs out counts, and the session completes once after it is saved`() =
        runTest(dispatcher) {
            val saved = CompletableDeferred<Unit>()
            coEvery { submit(any()) } coAnswers {
                saved.await()
                SubmitTrueOrFalseAnswerResponse(StepOutcome.CORRECT, isDisplayedTranslationCorrect = true)
            }
            val viewModel = viewModel()
            val events = completions(viewModel)
            advanceTimeBy(JUST_BEFORE_TIME_IS_UP_MS)

            viewModel.onAnswer(userAnsweredTrue = true)
            advanceTimeBy(PAST_THE_TIME_LIMIT_MS)
            assertEquals(emptyList<SessionNavigationEvent>(), events)

            saved.complete(Unit)
            advanceUntilIdle()

            assertEquals(listOf(SessionNavigationEvent.SessionComplete(correct = 1, incorrect = 0, skipped = 0, tipsUsed = 0)), events)
        }

    @Test
    fun `when the time runs out with no answer pending the session completes once`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            val events = completions(viewModel)

            advanceUntilIdle()

            assertEquals(listOf(SessionNavigationEvent.SessionComplete(correct = 0, incorrect = 0, skipped = 0, tipsUsed = 0)), events)
        }

    @Test
    fun `answering the last word completes the session once even when the time runs out as well`() =
        runTest(dispatcher) {
            coEvery { submit(any()) } returns SubmitTrueOrFalseAnswerResponse(StepOutcome.INCORRECT, isDisplayedTranslationCorrect = true)
            val viewModel = viewModel()
            val events = completions(viewModel)
            advanceTimeBy(JUST_BEFORE_TIME_IS_UP_MS)
            viewModel.onAnswer(userAnsweredTrue = false)
            advanceTimeBy(1)

            viewModel.onAnswer(userAnsweredTrue = false)
            advanceUntilIdle()

            assertEquals(listOf(SessionNavigationEvent.SessionComplete(correct = 0, incorrect = 2, skipped = 0, tipsUsed = 0)), events)
        }
}
