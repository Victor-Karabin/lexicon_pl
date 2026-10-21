package com.lexicon.presentation.passage

import androidx.lifecycle.SavedStateHandle
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.passage.Passage
import com.lexicon.interactors.passage.PassageGapResult
import com.lexicon.interactors.passage.PassageSegment
import com.lexicon.interactors.passage.PassageSentence
import com.lexicon.interactors.passage.PassageSessionResult
import com.lexicon.interactors.passage.StartPassageSessionRequest
import com.lexicon.interactors.passage.StartPassageSessionUseCase
import com.lexicon.interactors.passage.SubmitPassageAnswersResponse
import com.lexicon.interactors.passage.SubmitPassageAnswersUseCase
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.TRAINING_WORDS_ARG
import com.lexicon.presentation.common.asTrainingWordsArgument
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PassageViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers =
        object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

    private val passage = Passage(
        level = "A1",
        sentences = persistentListOf(
            PassageSentence(persistentListOf(PassageSegment.Text("Czytam "), PassageSegment.Gap(answer = "książkę", word = "książka"))),
        ),
    )

    private val submitAnswers: SubmitPassageAnswersUseCase = mockk()
    private val results = LastSessionResultsHolder()
    private val request = slot<StartPassageSessionRequest>()

    @Before
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(vocabularyIds: List<Long>? = null): PassageViewModel {
        val start: StartPassageSessionUseCase = mockk {
            coEvery { this@mockk(capture(request)) } returns PassageSessionResult.Ready("session-1", passage, persistentListOf())
        }
        val arguments = vocabularyIds?.let { mapOf(TRAINING_WORDS_ARG to it.asTrainingWordsArgument()) }.orEmpty()
        return PassageViewModel(
            savedStateHandle = SavedStateHandle(arguments),
            startSession = start,
            submitAnswers = submitAnswers,
            lastSessionResultsHolder = results,
            speechSynthesizer = mockk(relaxed = true),
            dispatchers = dispatchers,
        )
    }

    @Test
    fun `a lesson's word list reaches the session request`() =
        runTest(testDispatcher) {
            viewModel(vocabularyIds = listOf(7L, 11L, 13L))
            advanceUntilIdle()

            assertEquals(listOf(7L, 11L, 13L), request.captured.vocabularyIds)
        }

    @Test
    fun `an unscoped passage draws from the whole study set`() =
        runTest(testDispatcher) {
            viewModel()
            advanceUntilIdle()

            assertEquals(emptyList<Long>(), request.captured.vocabularyIds)
        }

    @Test
    fun `pressing Check twice before the first check returns submits the passage once`() =
        runTest(testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            coEvery { submitAnswers(any()) } coAnswers {
                gate.await()
                SubmitPassageAnswersResponse(listOf(PassageGapResult("książkę", "książkę", "a book", true)))
            }
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onAnswerChanged(0, "książkę")

            viewModel.onCheck()
            viewModel.onCheck()
            advanceUntilIdle()
            gate.complete(Unit)
            advanceUntilIdle()

            coVerify(exactly = 1) { submitAnswers(any()) }
            assertTrue(viewModel.uiState.value.isChecked)
        }

    @Test
    fun `pressing Check again once the passage is checked does not submit it again`() =
        runTest(testDispatcher) {
            coEvery { submitAnswers(any()) } returns
                SubmitPassageAnswersResponse(listOf(PassageGapResult("książkę", "książkę", "a book", true)))
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onAnswerChanged(0, "książkę")

            viewModel.onCheck()
            advanceUntilIdle()
            viewModel.onCheck()
            advanceUntilIdle()

            coVerify(exactly = 1) { submitAnswers(any()) }
        }
}
