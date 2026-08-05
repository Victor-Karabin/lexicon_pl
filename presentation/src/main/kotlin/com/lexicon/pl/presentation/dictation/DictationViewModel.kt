package com.lexicon.pl.presentation.dictation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.pl.android.SpeechSynthesizer
import com.lexicon.pl.common.DispatcherProvider
import com.lexicon.pl.interactors.dictation.DictationStepOutcome
import com.lexicon.pl.interactors.dictation.DictationStepResponse
import com.lexicon.pl.interactors.dictation.StartDictationSessionRequest
import com.lexicon.pl.interactors.dictation.StartDictationSessionUseCase
import com.lexicon.pl.interactors.dictation.SubmitDictationAnswerRequest
import com.lexicon.pl.interactors.dictation.SubmitDictationAnswerUseCase
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
class DictationViewModel @Inject constructor(
    private val startDictationSession: StartDictationSessionUseCase,
    private val submitDictationAnswer: SubmitDictationAnswerUseCase,
    private val speechSynthesizer: SpeechSynthesizer,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DictationUiState())
    val uiState: StateFlow<DictationUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<DictationNavigationEvent>()
    val navigationEvents: SharedFlow<DictationNavigationEvent> = _navigationEvents.asSharedFlow()

    private lateinit var sessionId: String
    private var steps: List<DictationStepResponse> = emptyList()
    private var correctCount = 0
    private var incorrectCount = 0
    private var skippedCount = 0

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
        _uiState.update { it.copy(tipUsed = true, revealedAnswer = step.expectedText) }
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

    private fun submitCurrentStep(submittedText: String, skipped: Boolean) {
        val step = currentStepOrNull() ?: return
        val state = _uiState.value
        viewModelScope.launch(dispatchers.io) {
            val response = submitDictationAnswer(
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
            applyOutcome(response.outcome, response.expectedText)
        }
    }

    private suspend fun applyOutcome(outcome: DictationStepOutcome, expectedText: String) {
        when (outcome) {
            DictationStepOutcome.CORRECT -> {
                correctCount++
                _uiState.update { it.copy(answerState = DictationAnswerState.CORRECT) }
                delay(CORRECT_ANSWER_ADVANCE_DELAY_MS)
                advanceToNextStep()
            }
            DictationStepOutcome.INCORRECT -> {
                incorrectCount++
                _uiState.update {
                    it.copy(answerState = DictationAnswerState.INCORRECT, revealedAnswer = expectedText)
                }
            }
            DictationStepOutcome.SKIPPED -> {
                skippedCount++
                _uiState.update {
                    it.copy(answerState = DictationAnswerState.SKIPPED, revealedAnswer = expectedText)
                }
            }
        }
    }

    /** Called from the UI's "Next" button after an Incorrect/Skipped step. */
    fun onNext() {
        if (!_uiState.value.awaitingNext) return
        viewModelScope.launch(dispatchers.io) { advanceToNextStep() }
    }

    private suspend fun advanceToNextStep() {
        val nextIndex = _uiState.value.stepIndex + 1
        if (nextIndex >= steps.size) {
            _uiState.update { it.copy(isSessionComplete = true) }
            _navigationEvents.emit(
                DictationNavigationEvent.SessionComplete(correctCount, incorrectCount, skippedCount),
            )
            return
        }
        _uiState.update {
            DictationUiState(isLoading = false, stepIndex = nextIndex, totalSteps = steps.size)
        }
        speakCurrentStep()
    }

    private fun currentStepOrNull(): DictationStepResponse? = steps.getOrNull(_uiState.value.stepIndex)
}
