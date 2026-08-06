package com.lexicon.presentation.dictation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.android.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.dictation.DictationStepOutcome
import com.lexicon.interactors.dictation.DictationStepResponse
import com.lexicon.interactors.dictation.StartDictationSessionRequest
import com.lexicon.interactors.dictation.StartDictationSessionUseCase
import com.lexicon.interactors.dictation.SubmitDictationAnswerRequest
import com.lexicon.interactors.dictation.SubmitDictationAnswerUseCase
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.WordResultEntry
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

private const val CORRECT_ANSWER_ADVANCE_DELAY_MS = 500L
private const val SKIPPED_ANSWER_ADVANCE_DELAY_MS = 700L

@HiltViewModel
class DictationViewModel
    @Inject
    constructor(
        private val startDictationSession: StartDictationSessionUseCase,
        private val submitDictationAnswer: SubmitDictationAnswerUseCase,
        private val speechSynthesizer: SpeechSynthesizer,
        private val dispatchers: DispatcherProvider,
        private val lastSessionResultsHolder: LastSessionResultsHolder,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DictationUiState())
        val uiState: StateFlow<DictationUiState> = _uiState.asStateFlow()

        private val _navigationEvents = MutableSharedFlow<SessionNavigationEvent>()
        val navigationEvents: SharedFlow<SessionNavigationEvent> = _navigationEvents.asSharedFlow()

        private lateinit var sessionId: String
        private var steps: List<DictationStepResponse> = emptyList()
        private var correctCount = 0
        private var incorrectCount = 0
        private var skippedCount = 0
        private var tipsUsedCount = 0
        private val wordResults = mutableListOf<WordResultEntry>()

        init {
            startSession()
        }

        private fun startSession() {
            viewModelScope.launch(dispatchers.io) {
                val response = startDictationSession(StartDictationSessionRequest())
                sessionId = response.sessionId
                steps = response.steps
                _uiState.update {
                    DictationUiState(isLoading = false, stepIndex = 0, totalSteps = steps.size)
                }
                speakCurrentStep()
            }
        }

        fun onReplayAudio() {
            viewModelScope.launch(dispatchers.io) { speakCurrentStep() }
        }

        private suspend fun speakCurrentStep() {
            val step = currentStepOrNull() ?: return
            speechSynthesizer.speak(step.expectedText)
        }

        fun onAnswerChanged(text: String) {
            if (!_uiState.value.isEditable) return
            _uiState.update { it.copy(answerText = text) }
        }

        fun onTipRequested() {
            val state = _uiState.value
            if (!state.canUseTip) return
            val step = currentStepOrNull() ?: return
            tipsUsedCount++
            _uiState.update { it.copy(tipUsed = true, tipTranslation = step.translationText) }
        }

        fun onCheck() {
            val state = _uiState.value
            if (!state.canCheck) return
            submitCurrentStep(submittedText = state.answerText, skipped = false)
        }

        fun onSkip() {
            val state = _uiState.value
            if (!state.canSkip) return
            submitCurrentStep(submittedText = "", skipped = true)
        }

        private fun submitCurrentStep(
            submittedText: String,
            skipped: Boolean,
        ) {
            val step = currentStepOrNull() ?: return
            val state = _uiState.value
            _uiState.update { it.copy(isSubmitting = true) }
            viewModelScope.launch(dispatchers.io) {
                val response =
                    submitDictationAnswer(
                        SubmitDictationAnswerRequest(
                            sessionId = sessionId,
                            stepIndex = step.stepIndex,
                            vocabularyItemId = step.vocabularyItemId,
                            expectedText = step.expectedText,
                            submittedText = submittedText,
                            tipUsed = state.tipUsed,
                            skipped = skipped,
                        ),
                    )
                applyOutcome(response.outcome, response.expectedText, state.tipUsed)
            }
        }

        private suspend fun applyOutcome(
            outcome: DictationStepOutcome,
            expectedText: String,
            tipUsed: Boolean,
        ) {
            val step = currentStepOrNull()
            when (outcome) {
                DictationStepOutcome.CORRECT -> {
                    correctCount++
                    step?.let {
                        wordResults += WordResultEntry(it.expectedText, it.translationText, AnswerState.CORRECT, tipUsed)
                    }
                    _uiState.update { it.copy(answerState = AnswerState.CORRECT) }
                    delay(CORRECT_ANSWER_ADVANCE_DELAY_MS)
                    advanceToNextStep()
                }
                DictationStepOutcome.INCORRECT -> {
                    incorrectCount++
                    step?.let {
                        wordResults += WordResultEntry(it.expectedText, it.translationText, AnswerState.INCORRECT, tipUsed)
                    }
                    // Waits for a manual Next tap, so isSubmitting must release now rather than on advance.
                    _uiState.update {
                        it.copy(answerState = AnswerState.INCORRECT, revealedAnswer = expectedText, isSubmitting = false)
                    }
                }
                DictationStepOutcome.SKIPPED -> {
                    skippedCount++
                    step?.let {
                        wordResults += WordResultEntry(it.expectedText, it.translationText, AnswerState.SKIPPED, tipUsed)
                    }
                    _uiState.update {
                        it.copy(answerState = AnswerState.SKIPPED, revealedAnswer = expectedText)
                    }
                    delay(SKIPPED_ANSWER_ADVANCE_DELAY_MS)
                    advanceToNextStep()
                }
            }
        }

        /** Called from the UI's "Next" button after an Incorrect/Skipped step. */
        fun onNext() {
            if (!_uiState.value.awaitingNext) return
            _uiState.update { it.copy(isSubmitting = true) }
            viewModelScope.launch(dispatchers.io) { advanceToNextStep() }
        }

        private suspend fun advanceToNextStep() {
            val nextIndex = _uiState.value.stepIndex + 1
            if (nextIndex >= steps.size) {
                _uiState.update { it.copy(isSessionComplete = true, isSubmitting = false) }
                lastSessionResultsHolder.wordResults = wordResults.toList()
                _navigationEvents.emit(
                    SessionNavigationEvent.SessionComplete(correctCount, incorrectCount, skippedCount, tipsUsedCount),
                )
                return
            }
            // A fresh DictationUiState() already defaults isSubmitting to false.
            _uiState.update {
                DictationUiState(isLoading = false, stepIndex = nextIndex, totalSteps = steps.size)
            }
            speakCurrentStep()
        }

        private fun currentStepOrNull(): DictationStepResponse? = steps.getOrNull(_uiState.value.stepIndex)
    }
