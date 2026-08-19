package com.lexicon.presentation.dictation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.dictation.DictationStepResponse
import com.lexicon.interactors.dictation.StartDictationSessionRequest
import com.lexicon.interactors.dictation.StartDictationSessionUseCase
import com.lexicon.interactors.dictation.SubmitDictationAnswerRequest
import com.lexicon.interactors.dictation.SubmitDictationAnswerUseCase
import com.lexicon.model.training.StepOutcome
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.WordResultEntry
import com.lexicon.presentation.common.trainingVocabularyIds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val CORRECT_ANSWER_ADVANCE_DELAY_MS = 500L
private const val SKIPPED_ANSWER_ADVANCE_DELAY_MS = 700L

class DictationViewModel(
    savedStateHandle: SavedStateHandle,
    private val startDictationSession: StartDictationSessionUseCase,
    private val submitDictationAnswer: SubmitDictationAnswerUseCase,
    private val speechSynthesizer: SpeechSynthesizer,
    private val dispatchers: DispatcherProvider,
    private val lastSessionResultsHolder: LastSessionResultsHolder,
) : ViewModel() {
    private val vocabularyIds = savedStateHandle.trainingVocabularyIds()

    private val _uiState = MutableStateFlow<DictationUiState>(DictationUiState.Loading)
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
            val response = startDictationSession(StartDictationSessionRequest(vocabularyIds = vocabularyIds))
            sessionId = response.sessionId
            steps = response.steps
            _uiState.update {
                DictationUiState.Loaded(stepIndex = 0, totalSteps = steps.size)
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
        updateLoaded { state -> if (state.isEditable) state.copy(answerText = text) else state }
    }

    fun onTipRequested() {
        val state = _uiState.value as? DictationUiState.Loaded ?: return
        if (!state.canUseTip) return
        val step = currentStepOrNull() ?: return
        tipsUsedCount++
        updateLoaded { it.copy(tipUsed = true, tipTranslation = step.translationText) }
    }

    fun onCheck() {
        val state = _uiState.value as? DictationUiState.Loaded ?: return
        if (!state.canCheck) return
        submitCurrentStep(submittedText = state.answerText, skipped = false)
    }

    fun onSkip() {
        val state = _uiState.value as? DictationUiState.Loaded ?: return
        if (!state.canSkip) return
        submitCurrentStep(submittedText = "", skipped = true)
    }

    private fun submitCurrentStep(
        submittedText: String,
        skipped: Boolean,
    ) {
        val step = currentStepOrNull() ?: return
        val state = _uiState.value as? DictationUiState.Loaded ?: return
        updateLoaded { it.copy(isSubmitting = true) }
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
        outcome: StepOutcome,
        expectedText: String,
        tipUsed: Boolean,
    ) {
        val step = currentStepOrNull()
        when (outcome) {
            StepOutcome.CORRECT -> {
                correctCount++
                step?.let {
                    wordResults += WordResultEntry(it.expectedText, it.translationText, AnswerState.Correct, tipUsed)
                }
                updateLoaded { it.copy(answerState = AnswerState.Correct) }
                delay(CORRECT_ANSWER_ADVANCE_DELAY_MS)
                advanceToNextStep()
            }
            StepOutcome.INCORRECT -> {
                incorrectCount++
                step?.let {
                    wordResults +=
                        WordResultEntry(it.expectedText, it.translationText, AnswerState.Incorrect(expectedText), tipUsed)
                }
                updateLoaded {
                    it.copy(answerState = AnswerState.Incorrect(expectedText), isSubmitting = false)
                }
            }
            StepOutcome.SKIPPED -> {
                skippedCount++
                step?.let {
                    wordResults +=
                        WordResultEntry(it.expectedText, it.translationText, AnswerState.Skipped(expectedText), tipUsed)
                }
                updateLoaded {
                    it.copy(answerState = AnswerState.Skipped(expectedText))
                }
                delay(SKIPPED_ANSWER_ADVANCE_DELAY_MS)
                advanceToNextStep()
            }

            StepOutcome.SEEN -> Unit
        }
    }

    fun onNext() {
        val state = _uiState.value as? DictationUiState.Loaded ?: return
        if (!state.awaitingNext) return
        updateLoaded { it.copy(isSubmitting = true) }
        viewModelScope.launch(dispatchers.io) { advanceToNextStep() }
    }

    private suspend fun advanceToNextStep() {
        val state = _uiState.value as? DictationUiState.Loaded ?: return
        val nextIndex = state.stepIndex + 1
        if (nextIndex >= steps.size) {
            updateLoaded { it.copy(isSessionComplete = true, isSubmitting = false) }
            lastSessionResultsHolder.wordResults = wordResults.toList()
            _navigationEvents.emit(
                SessionNavigationEvent.SessionComplete(correctCount, incorrectCount, skippedCount, tipsUsedCount),
            )
            return
        }
        _uiState.update {
            DictationUiState.Loaded(stepIndex = nextIndex, totalSteps = steps.size)
        }
        speakCurrentStep()
    }

    private fun currentStepOrNull(): DictationStepResponse? {
        val state = _uiState.value as? DictationUiState.Loaded ?: return null
        return steps.getOrNull(state.stepIndex)
    }

    private inline fun updateLoaded(transform: (DictationUiState.Loaded) -> DictationUiState.Loaded) {
        _uiState.update { current -> if (current is DictationUiState.Loaded) transform(current) else current }
    }
}
