package com.lexicon.presentation.pronunciation

import androidx.lifecycle.SavedStateHandle
import com.lexicon.boundary.SpeechRecognitionFailed
import com.lexicon.boundary.SpeechRecognitionFailureReason
import com.lexicon.boundary.SpeechRecognizerService
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.pronunciation.PronunciationSentencesResult
import com.lexicon.interactors.pronunciation.PronunciationSessionResponse
import com.lexicon.interactors.pronunciation.PronunciationStepResponse
import com.lexicon.interactors.pronunciation.StartPronunciationSentencesUseCase
import com.lexicon.interactors.pronunciation.StartPronunciationSessionRequest
import com.lexicon.interactors.pronunciation.StartPronunciationSessionUseCase
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.TRAINING_WORDS_ARG
import com.lexicon.presentation.common.asTrainingWordsArgument
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
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
class PronunciationViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers =
        object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

    private val recognizer: SpeechRecognizerService = mockk()

    @Before
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(): PronunciationViewModel {
        val start: StartPronunciationSessionUseCase = mockk {
            coEvery { this@mockk(any()) } returns
                PronunciationSessionResponse("session-1", listOf(PronunciationStepResponse(0, 1L, "kot", "cat", "kɔt")))
        }
        return PronunciationViewModel(
            savedStateHandle = SavedStateHandle(),
            startSessionUseCase = start,
            startSentencesUseCase = mockk(),
            submitResultUseCase = mockk(),
            speechSynthesizer = mockk(relaxed = true),
            speechRecognizerService = recognizer,
            audioPlayer = mockk(relaxed = true),
            dispatchers = dispatchers,
            lastSessionResultsHolder = LastSessionResultsHolder(),
        )
    }

    private fun failingWith(
        message: String,
        reason: SpeechRecognitionFailureReason,
    ): RecognitionErrorType? {
        coEvery { recognizer.recognize() } throws SpeechRecognitionFailed(message, reason)
        val viewModel = viewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onRecordRequested()
        testDispatcher.scheduler.advanceUntilIdle()
        return (viewModel.uiState.value as PronunciationUiState.Loaded).recognitionError
    }

    @Test
    fun `an unavailable recogniser is reported as unavailable`() =
        runTest(testDispatcher) {
            assertEquals(RecognitionErrorType.UNAVAILABLE, failingWith("Recognition error 5", SpeechRecognitionFailureReason.UNAVAILABLE))
        }

    @Test
    fun `a failure is not taken for unavailability because its message says so`() =
        runTest(testDispatcher) {
            assertEquals(
                RecognitionErrorType.FAILED,
                failingWith("The word was not available in the transcript", SpeechRecognitionFailureReason.FAILED),
            )
        }

    @Test
    fun `an unavailable recogniser is still unavailable when its message is worded differently`() =
        runTest(testDispatcher) {
            assertEquals(RecognitionErrorType.UNAVAILABLE, failingWith("No engine", SpeechRecognitionFailureReason.UNAVAILABLE))
        }

    @Test
    fun `a lesson's word list reaches the sentences request`() =
        runTest(testDispatcher) {
            val request = slot<StartPronunciationSessionRequest>()
            val sentences: StartPronunciationSentencesUseCase = mockk {
                coEvery { this@mockk(capture(request)) } returns
                    PronunciationSentencesResult.Ready(
                        PronunciationSessionResponse(
                            "session-1",
                            listOf(PronunciationStepResponse(0, 1L, "To jest kot.", "To jest kot.", "")),
                        ),
                    )
            }

            PronunciationViewModel(
                savedStateHandle = SavedStateHandle(
                    mapOf(
                        PRONUNCIATION_SENTENCES_ARG to "true",
                        TRAINING_WORDS_ARG to listOf(7L, 11L).asTrainingWordsArgument(),
                    ),
                ),
                startSessionUseCase = mockk(),
                startSentencesUseCase = sentences,
                submitResultUseCase = mockk(),
                speechSynthesizer = mockk(relaxed = true),
                speechRecognizerService = recognizer,
                audioPlayer = mockk(relaxed = true),
                dispatchers = dispatchers,
                lastSessionResultsHolder = LastSessionResultsHolder(),
            )
            advanceUntilIdle()

            assertEquals(listOf(7L, 11L), request.captured.vocabularyIds)
        }
}
