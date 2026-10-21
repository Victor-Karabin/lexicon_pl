package com.lexicon.presentation.countries

import app.cash.turbine.test
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.countries.Country
import com.lexicon.interactors.countries.NextCountryUseCase
import com.lexicon.interactors.countries.SubmitCountryAnswersUseCase
import com.lexicon.interactors.settings.GetStepCountUseCase
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.SessionNavigationEvent
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CountryTrainingStepCountTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers =
        object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

    private val nextCountry: NextCountryUseCase = mockk()
    private val getStepCount: GetStepCountUseCase = mockk { coEvery { this@mockk() } returns STEPS }

    @Before
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        CountryTrainingViewModel(
            nextCountry = nextCountry,
            submitAnswers = mockk<SubmitCountryAnswersUseCase>(),
            speechSynthesizer = mockk(relaxed = true),
            lastSessionResultsHolder = LastSessionResultsHolder(),
            recordStudy = mockk(relaxed = true),
            getStepCount = getStepCount,
            dispatchers = dispatchers,
        )

    private fun pool() =
        (0 until STEPS + 5).map {
            Country("C$it", "Country $it", listOf("Kraj $it"), null, null, "Stolica $it", "Capital $it")
        }

    @Test
    fun `the session is as long as the Steps per session setting`() =
        runTest(testDispatcher) {
            val pool = pool()
            coEvery { nextCountry(any(), any()) } answers { pool.first { it.code !in firstArg<Set<String>>() } }

            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals(STEPS, viewModel.uiState.value.totalSteps)
        }

    @Test
    fun `the session ends after as many countries as the setting says`() =
        runTest(testDispatcher) {
            val pool = pool()
            coEvery { nextCountry(any(), any()) } answers { pool.first { it.code !in firstArg<Set<String>>() } }
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.navigationEvents.test {
                repeat(STEPS - 1) {
                    viewModel.onNext()
                    advanceUntilIdle()
                    expectNoEvents()
                }

                viewModel.onNext()
                advanceUntilIdle()

                awaitItem() as SessionNavigationEvent.SessionComplete
            }
        }

    private companion object {
        const val STEPS = 3
    }
}
