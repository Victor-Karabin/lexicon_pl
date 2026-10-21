package com.lexicon.presentation.declension

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.declension.DeclensionColumn
import com.lexicon.interactors.declension.DeclensionStep
import com.lexicon.interactors.declension.DeclensionTable
import com.lexicon.interactors.declension.DeclensionVariant
import com.lexicon.interactors.declension.LoadDeclensionCoursesUseCase
import com.lexicon.interactors.declension.NextDeclensionTableUseCase
import com.lexicon.interactors.declension.SubmitDeclensionAnswerUseCase
import com.lexicon.interactors.settings.GetStepCountUseCase
import com.lexicon.model.vocabulary.GrammaticalCase
import com.lexicon.model.vocabulary.PartOfSpeech
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.SessionNavigationEvent
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
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
class DeclensionViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers =
        object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

    private val table = DeclensionTable(
        word = "kot",
        translation = "cat",
        partOfSpeech = PartOfSpeech.NOUN,
        gender = null,
        column = DeclensionColumn.SINGULAR,
        steps = persistentListOf(
            DeclensionStep(DeclensionVariant("kot", DeclensionColumn.SINGULAR, GrammaticalCase.GENITIVE), persistentListOf("kota")),
        ),
    )

    private val nextQuestion: NextDeclensionTableUseCase = mockk { coEvery { this@mockk(any()) } returns table }
    private val getStepCount: GetStepCountUseCase = mockk { coEvery { this@mockk() } returns STEPS }

    @Before
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        DeclensionViewModel(
            savedStateHandle = SavedStateHandle(),
            nextQuestion = nextQuestion,
            submitAnswer = mockk<SubmitDeclensionAnswerUseCase>(),
            loadCourses = mockk<LoadDeclensionCoursesUseCase>(),
            speechSynthesizer = mockk(relaxed = true),
            lastSessionResultsHolder = LastSessionResultsHolder(),
            recordStudy = mockk(relaxed = true),
            getStepCount = getStepCount,
            dispatchers = dispatchers,
        )

    @Test
    fun `the session is as long as the Steps per session setting`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals(STEPS, viewModel.uiState.value.totalSteps)
        }

    @Test
    fun `the session ends after as many tables as the setting says`() =
        runTest(testDispatcher) {
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
