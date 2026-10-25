package com.lexicon.presentation.numbers

import app.cash.turbine.test
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.numbers.NextNumberUseCase
import com.lexicon.interactors.numbers.NumberQuestion
import com.lexicon.interactors.numbers.SubmitNumberAnswerUseCase
import com.lexicon.interactors.settings.GetStepCountUseCase
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.SessionNavigationEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
class NumberTrainingViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers =
        object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

    private val nextNumber: NextNumberUseCase = mockk()
    private val getStepCount: GetStepCountUseCase = mockk { coEvery { this@mockk() } returns STEPS }

    @Before
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        NumberTrainingViewModel(
            nextNumber = nextNumber,
            submitAnswer = mockk<SubmitNumberAnswerUseCase> { coEvery { this@mockk(any(), any()) } returns AnswerVerdict.CORRECT },
            speechSynthesizer = mockk(relaxed = true),
            lastSessionResultsHolder = LastSessionResultsHolder(),
            recordStudy = mockk(relaxed = true),
            getStepCount = getStepCount,
            dispatchers = dispatchers,
        )

    private fun TestScope.answer(viewModel: NumberTrainingViewModel) {
        viewModel.onAnswerChanged("jeden")
        viewModel.onCheck()
        advanceUntilIdle()
    }

    @Test
    fun `the session is as long as the Steps per session setting`() =
        runTest(testDispatcher) {
            coEvery { nextNumber(any(), any()) } answers { NumberQuestion.of(firstArg<Set<*>>().size.toLong() + 1) }

            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals(STEPS, viewModel.uiState.value.totalSteps)
        }

    @Test
    fun `the session ends after as many numbers as the setting says`() =
        runTest(testDispatcher) {
            coEvery { nextNumber(any(), any()) } answers { NumberQuestion.of(firstArg<Set<*>>().size.toLong() + 1) }
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.navigationEvents.test {
                repeat(STEPS - 1) {
                    answer(viewModel)
                    viewModel.onNext()
                    advanceUntilIdle()
                    expectNoEvents()
                }
                assertEquals(STEPS - 1, viewModel.uiState.value.stepIndex)

                answer(viewModel)
                viewModel.onNext()
                advanceUntilIdle()

                awaitItem() as SessionNavigationEvent.SessionComplete
            }
        }

    @Test
    fun `Next does nothing while the number is unanswered`() =
        runTest(testDispatcher) {
            coEvery { nextNumber(any(), any()) } answers { NumberQuestion.of(firstArg<Set<*>>().size.toLong() + 1) }
            val viewModel = viewModel()
            advanceUntilIdle()
            answer(viewModel)
            viewModel.onNext()
            advanceUntilIdle()

            viewModel.onNext()
            advanceUntilIdle()

            assertEquals(1, viewModel.uiState.value.stepIndex)
            coVerify(exactly = 2) { nextNumber(any(), any()) }
        }

    private companion object {
        const val STEPS = 3
    }
}
