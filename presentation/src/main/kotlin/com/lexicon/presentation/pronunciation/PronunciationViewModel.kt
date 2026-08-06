package com.lexicon.presentation.pronunciation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.android.SpeechRecognitionFailed
import com.lexicon.android.SpeechRecognizerService
import com.lexicon.android.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.pronunciation.PronunciationStepOutcome
import com.lexicon.interactors.pronunciation.PronunciationStepResponse
import com.lexicon.interactors.pronunciation.StartPronunciationSessionRequest
import com.lexicon.interactors.pronunciation.StartPronunciationSessionUseCase
import com.lexicon.interactors.pronunciation.SubmitPronunciationResultRequest
import com.lexicon.interactors.pronunciation.SubmitPronunciationResultUseCase
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.SessionNavigationEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val CORRECT_ANSWER_ADVANCE_DELAY_MS = 400L

@HiltViewModel
class PronunciationViewModel
    @Inject
    constructor(
        private val startSessionUseCase: StartPronunciationSessionUseCase,
        private val submitResultUseCase: SubmitPronunciationResultUseCase,
        private val speechSynthesizer: SpeechSynthesizer,
        private val speechRecognizerService: SpeechRecognizerService,
        private val dispatchers: DispatcherProvider,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<PronunciationUiState>(PronunciationUiState.Loading)
        val uiState: StateFlow<PronunciationUiState> = _uiState.asStateFlow()

        private val _navigationEvents = MutableSharedFlow<SessionNavigationEvent>()
        val navigationEvents: SharedFlow<SessionNavigationEvent> = _navigationEvents.asSharedFlow()

        private lateinit var sessionId: String
        private var steps: List<PronunciationStepResponse> = emptyList()
        private var correctCount = 0
        private var incorrectCount = 0
        private var skippedCount = 0
        private var tipsUsedCount = 0

        init {
            startSession()
        }

        private fun startSession() {
            viewModelScope.launch(dispatchers.io) {
                val response = startSessionUseCase(StartPronunciationSessionRequest())
                sessionId = response.sessionId
                steps = response.steps
                _uiState.update { PronunciationUiState.Loaded(stepIndex = 0, totalSteps = steps.size) }
                speakReferenceAudio()
            }
        }

        fun onReplayReferenceAudio() {
            viewModelScope.launch(dispatchers.io) { speakReferenceAudio() }
        }

        private suspend fun speakReferenceAudio() {
            val step = currentStepOrNull() ?: return
            speechSynthesizer.speak(step.expectedText)
        }

        fun onTipRequested() {
            val state = _uiState.value as? PronunciationUiState.Loaded ?: return
            if (!state.canUseTip) return
            val step = currentStepOrNull() ?: return
            tipsUsedCount++
            updateLoaded { it.copy(tipUsed = true, tipTranslation = step.clueText) }
        }

        fun onRecordRequested() {
            val state = _uiState.value as? PronunciationUiState.Loaded ?: return
            if (!state.canRecord) return
            updateLoaded { it.copy(recordingState = RecordingState.RECORDING) }
            viewModelScope.launch(dispatchers.io) {
                try {
                    val result = speechRecognizerService.recognize()
                    updateLoaded { it.copy(recordingState = RecordingState.PROCESSING, recognizedText = result.recognizedText) }
                    submitCurrentStep(recognizedText = result.recognizedText, confidence = result.confidence, skipped = false)
                } catch (failure: SpeechRecognitionFailed) {
                    // Per spec: a failed recognition doesn't complete the step — reset so the user can retry.
                    updateLoaded { it.copy(recordingState = RecordingState.IDLE) }
                }
            }
        }

        fun onSkip() {
            val state = _uiState.value as? PronunciationUiState.Loaded ?: return
            if (!state.canSkip) return
            submitCurrentStep(recognizedText = "", confidence = null, skipped = true)
        }

        private fun submitCurrentStep(
            recognizedText: String,
            confidence: Float?,
            skipped: Boolean,
        ) {
            val step = currentStepOrNull() ?: return
            val state = _uiState.value as? PronunciationUiState.Loaded ?: return
            viewModelScope.launch(dispatchers.io) {
                val response =
                    submitResultUseCase(
                        SubmitPronunciationResultRequest(
                            sessionId = sessionId,
                            stepIndex = step.stepIndex,
                            vocabularyItemId = step.vocabularyItemId,
                            expectedText = step.expectedText,
                            recognizedText = recognizedText,
                            confidence = confidence,
                            tipUsed = state.tipUsed,
                            skipped = skipped,
                        ),
                    )
                applyOutcome(response.outcome, response.expectedText)
            }
        }

        private suspend fun applyOutcome(
            outcome: PronunciationStepOutcome,
            expectedText: String,
        ) {
            when (outcome) {
                PronunciationStepOutcome.CORRECT -> {
                    correctCount++
                    updateLoaded { it.copy(answerState = AnswerState.Correct) }
                    delay(CORRECT_ANSWER_ADVANCE_DELAY_MS)
                    advanceToNextStep()
                }
                PronunciationStepOutcome.INCORRECT -> {
                    incorrectCount++
                    updateLoaded { it.copy(answerState = AnswerState.Incorrect(expectedText)) }
                }
                PronunciationStepOutcome.SKIPPED -> {
                    skippedCount++
                    updateLoaded { it.copy(answerState = AnswerState.Skipped(expectedText)) }
                }
            }
        }

        /** Called from the UI's "Next" button after an Incorrect/Skipped step. */
        fun onNext() {
            val state = _uiState.value as? PronunciationUiState.Loaded ?: return
            if (!state.awaitingNext) return
            viewModelScope.launch(dispatchers.io) { advanceToNextStep() }
        }

        private suspend fun advanceToNextStep() {
            val state = _uiState.value as? PronunciationUiState.Loaded ?: return
            val nextIndex = state.stepIndex + 1
            if (nextIndex >= steps.size) {
                updateLoaded { it.copy(isSessionComplete = true) }
                _navigationEvents.emit(
                    SessionNavigationEvent.SessionComplete(correctCount, incorrectCount, skippedCount, tipsUsedCount),
                )
                return
            }
            _uiState.update { PronunciationUiState.Loaded(stepIndex = nextIndex, totalSteps = steps.size) }
            speakReferenceAudio()
        }

        private fun currentStepOrNull(): PronunciationStepResponse? {
            val state = _uiState.value as? PronunciationUiState.Loaded ?: return null
            return steps.getOrNull(state.stepIndex)
        }

        private inline fun updateLoaded(transform: (PronunciationUiState.Loaded) -> PronunciationUiState.Loaded) {
            _uiState.update { current -> if (current is PronunciationUiState.Loaded) transform(current) else current }
        }
    }
