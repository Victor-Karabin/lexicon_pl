package com.lexicon.presentation.fillword

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.fillword.FillwordCell
import com.lexicon.interactors.fillword.FillwordDirection
import com.lexicon.interactors.fillword.FillwordPuzzle
import com.lexicon.interactors.fillword.FillwordSessionResult
import com.lexicon.interactors.fillword.FillwordWord
import com.lexicon.interactors.fillword.StartFillwordSessionRequest
import com.lexicon.interactors.fillword.StartFillwordSessionUseCase
import com.lexicon.interactors.fillword.SubmitFillwordRequest
import com.lexicon.interactors.fillword.SubmitFillwordUseCase
import com.lexicon.interactors.training.RecordStudyActivityUseCase
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.TRAINING_WORDS_ARG
import com.lexicon.presentation.common.WordResultEntry
import com.lexicon.presentation.common.asTrainingWordsArgument
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
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
class FillwordViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers =
        object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

    private val puzzle = FillwordPuzzle(
        grid = persistentListOf(
            persistentListOf("K", "O", "T"),
            persistentListOf("A", "B", "C"),
            persistentListOf("D", "O", "M"),
        ),
        words = persistentListOf(
            FillwordWord("KOT", FillwordCell(0, 0), FillwordDirection.RIGHT),
            FillwordWord("DOM", FillwordCell(2, 0), FillwordDirection.RIGHT),
        ),
        translations = persistentMapOf("KOT" to "cat", "DOM" to "house"),
    )

    private val request = slot<StartFillwordSessionRequest>()
    private val submit: SubmitFillwordUseCase = mockk(relaxed = true)
    private val recordStudy: RecordStudyActivityUseCase = mockk(relaxed = true)
    private val results = LastSessionResultsHolder()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        Dispatchers.resetMain()
    }

    private fun viewModel(vocabularyIds: List<Long>? = null): FillwordViewModel {
        val start: StartFillwordSessionUseCase = mockk {
            coEvery { this@mockk(capture(request)) } returns FillwordSessionResult.Ready("session-1", puzzle)
        }
        val arguments = vocabularyIds?.let { mapOf(TRAINING_WORDS_ARG to it.asTrainingWordsArgument()) }.orEmpty()
        return FillwordViewModel(
            savedStateHandle = SavedStateHandle(arguments),
            startSession = start,
            submitFillword = submit,
            lastSessionResultsHolder = results,
            recordStudy = recordStudy,
            dispatchers = dispatchers,
        )
    }

    private fun FillwordViewModel.findKot() = onCellsTraced(FillwordCell(0, 0), FillwordCell(0, 2))

    private fun FillwordViewModel.findDom() = onCellsTraced(FillwordCell(2, 0), FillwordCell(2, 2))

    @Test
    fun `a lesson's word list reaches the session request`() =
        runTest(testDispatcher) {
            viewModel(vocabularyIds = listOf(7L, 11L, 13L))
            advanceUntilIdle()

            assertEquals(listOf(7L, 11L, 13L), request.captured.vocabularyIds)
        }

    @Test
    fun `an unscoped round draws from the whole study set`() =
        runTest(testDispatcher) {
            viewModel()
            advanceUntilIdle()

            assertEquals(emptyList<Long>(), request.captured.vocabularyIds)
        }

    @Test
    fun `finding the last word submits every word once`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.findKot()
            advanceUntilIdle()
            coVerify(exactly = 0) { submit(any()) }

            viewModel.findDom()
            advanceUntilIdle()

            coVerify(exactly = 1) { submit(SubmitFillwordRequest(sessionId = "session-1", foundWords = setOf("KOT", "DOM"))) }
            coVerify(exactly = 1) { submit(any()) }
        }

    @Test
    fun `checking submits exactly the words found so far`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.findKot()

            viewModel.onCheck()
            advanceUntilIdle()

            coVerify(exactly = 1) { submit(SubmitFillwordRequest(sessionId = "session-1", foundWords = setOf("KOT"))) }
            coVerify(exactly = 1) { submit(any()) }
        }

    @Test
    fun `checking with nothing found submits an empty set`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onCheck()
            advanceUntilIdle()

            coVerify(exactly = 1) { submit(SubmitFillwordRequest(sessionId = "session-1", foundWords = emptySet())) }
        }

    @Test
    fun `pressing Check twice before the first submit returns submits the round once`() =
        runTest(testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            coEvery { submit(any()) } coAnswers { gate.await() }
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.findKot()

            viewModel.onCheck()
            viewModel.onCheck()
            advanceUntilIdle()
            gate.complete(Unit)
            advanceUntilIdle()

            coVerify(exactly = 1) { submit(any()) }
        }

    @Test
    fun `pressing Check after the round is complete does not submit it again`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.findKot()
            viewModel.findDom()
            advanceUntilIdle()

            viewModel.onCheck()
            advanceUntilIdle()

            coVerify(exactly = 1) { submit(any()) }
        }

    @Test
    fun `pressing Check again once it has been checked does not submit it again`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.findKot()

            viewModel.onCheck()
            advanceUntilIdle()
            viewModel.onCheck()
            advanceUntilIdle()

            coVerify(exactly = 1) { submit(any()) }
        }

    @Test
    fun `the results still mark the words that were not found as incorrect`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.findKot()

            viewModel.onCheck()
            advanceUntilIdle()
            viewModel.onFinished()

            assertEquals(
                listOf(
                    WordResultEntry(word = "KOT", translation = "cat", outcome = AnswerState.Correct),
                    WordResultEntry(word = "DOM", translation = "house", outcome = AnswerState.Incorrect("DOM")),
                ),
                results.wordResults,
            )
        }

    @Test
    fun `tapping the grid still records study time`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onCellTapped(FillwordCell(0, 0))
            advanceUntilIdle()

            coVerify(atLeast = 1) { recordStudy() }
        }

    @Test
    fun `a submit already under way survives the screen being left`() =
        runTest(testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            var recorded = false
            coEvery { submit(any()) } coAnswers {
                gate.await()
                recorded = true
            }
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.findKot()
            viewModel.findDom()
            advanceUntilIdle()

            viewModel.viewModelScope.cancel()
            gate.complete(Unit)
            advanceUntilIdle()

            assertTrue(recorded)
        }

    @Test
    fun `finding the last word does not also record study time`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.findKot()
            advanceUntilIdle()
            coVerify(exactly = 1) { recordStudy() }

            viewModel.findDom()
            advanceUntilIdle()

            coVerify(exactly = 1) { recordStudy() }
            coVerify(exactly = 1) { submit(any()) }
        }

    @Test
    fun `checking does not also record study time`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.findKot()
            advanceUntilIdle()

            viewModel.onCheck()
            advanceUntilIdle()

            coVerify(exactly = 1) { recordStudy() }
            coVerify(exactly = 1) { submit(any()) }
        }

    @Test
    fun `a submit that fails does not crash and a later Check retries it`() =
        runTest(testDispatcher) {
            coEvery { submit(any()) } throws IllegalStateException("store unavailable") andThen Unit
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.findKot()

            viewModel.onCheck()
            advanceUntilIdle()
            viewModel.onCheck()
            advanceUntilIdle()

            coVerify(exactly = 2) { submit(SubmitFillwordRequest(sessionId = "session-1", foundWords = setOf("KOT"))) }
        }
}
