package com.lexicon.presentation.mix

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import com.lexicon.boundary.SpeechRecognitionFailed
import com.lexicon.boundary.SpeechRecognitionFailureReason
import com.lexicon.boundary.SpeechRecognitionResult
import com.lexicon.boundary.SpeechRecognizerService
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.mix.MixSessionResponse
import com.lexicon.interactors.mix.MixStep
import com.lexicon.interactors.mix.StartMixSessionUseCase
import com.lexicon.interactors.pronunciation.PronunciationStepResponse
import com.lexicon.interactors.puzzle.PuzzleStepResponse
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.pronunciation.RecognitionErrorType
import com.lexicon.presentation.pronunciation.RecordingState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MixViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers =
        object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

    private val startMixSession: StartMixSessionUseCase = mockk()
    private val recognizer: SpeechRecognizerService = mockk()
    private var logged = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>()) } answers { logged++ }
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } answers { logged++ }
        every { Log.w(any<String>(), any<Throwable>()) } answers { logged++ }
        every { Log.e(any<String>(), any<String>()) } answers { logged++ }
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } answers { logged++ }
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        Dispatchers.resetMain()
    }

    private fun viewModel(vararg steps: MixStep): MixViewModel {
        coEvery { startMixSession(any()) } returns MixSessionResponse("session-1", steps.toList())
        return MixViewModel(
            savedStateHandle = SavedStateHandle(),
            startMixSession = startMixSession,
            submitDictation = mockk(),
            submitDictationPuzzle = mockk(),
            submitPuzzle = mockk(),
            submitImageTest = mockk(),
            submitTrueOrFalse = mockk(),
            submitPronunciation = mockk(),
            speechSynthesizer = mockk(relaxed = true),
            speechRecognizerService = recognizer,
            dispatchers = dispatchers,
            lastSessionResultsHolder = LastSessionResultsHolder(),
        )
    }

    private fun loaded(viewModel: MixViewModel) = viewModel.uiState.value as MixUiState.Loaded

    private val pronunciation = MixStep.Pronunciation(0, PronunciationStepResponse(0, 1L, "kot", "cat", "kɔt"))

    private val puzzle = MixStep.Puzzle(0, PuzzleStepResponse(0, 1L, "praca", null, "work"))

    @Test
    fun `a recognition failure is logged and shown as a failed recording`() =
        runTest(testDispatcher) {
            coEvery { recognizer.recognize() } throws SpeechRecognitionFailed("it broke", SpeechRecognitionFailureReason.FAILED)
            val viewModel = viewModel(pronunciation)
            advanceUntilIdle()

            viewModel.onRecordRequested()
            advanceUntilIdle()

            assertEquals(RecognitionErrorType.FAILED, loaded(viewModel).recognitionError)
            assertEquals(RecordingState.IDLE, loaded(viewModel).recordingState)
            assertTrue("the failure should be logged", logged > 0)
        }

    @Test
    fun `an unavailable recogniser is shown as unavailable whatever its message says`() =
        runTest(testDispatcher) {
            coEvery { recognizer.recognize() } throws SpeechRecognitionFailed("boom", SpeechRecognitionFailureReason.UNAVAILABLE)
            val viewModel = viewModel(pronunciation)
            advanceUntilIdle()

            viewModel.onRecordRequested()
            advanceUntilIdle()

            assertEquals(RecognitionErrorType.UNAVAILABLE, loaded(viewModel).recognitionError)
        }

    @Test
    fun `recording again clears the earlier recognition error`() =
        runTest(testDispatcher) {
            coEvery { recognizer.recognize() } throws SpeechRecognitionFailed("boom", SpeechRecognitionFailureReason.FAILED)
            val viewModel = viewModel(pronunciation)
            advanceUntilIdle()
            viewModel.onRecordRequested()
            advanceUntilIdle()

            coEvery { recognizer.recognize() } returns SpeechRecognitionResult("kot", 0.9f, null)
            viewModel.onRecordRequested()
            advanceUntilIdle()

            assertNull(loaded(viewModel).recognitionError)
            assertEquals(RecordingState.RECORDED, loaded(viewModel).recordingState)
        }

    @Test
    fun `a tile that is already placed cannot be placed again`() =
        runTest(testDispatcher) {
            val viewModel = viewModel(puzzle)
            advanceUntilIdle()
            val tile = loaded(viewModel).stepTiles.first()

            viewModel.onTileSelected(tile)
            viewModel.onTileSelected(tile)

            assertEquals(listOf(tile), loaded(viewModel).placedTiles)
        }

    @Test
    fun `every tile can be placed once`() =
        runTest(testDispatcher) {
            val viewModel = viewModel(puzzle)
            advanceUntilIdle()
            val tiles = loaded(viewModel).stepTiles

            tiles.forEach(viewModel::onTileSelected)

            assertEquals(tiles, loaded(viewModel).placedTiles)
            assertTrue(loaded(viewModel).availableTiles.isEmpty())
        }
}
