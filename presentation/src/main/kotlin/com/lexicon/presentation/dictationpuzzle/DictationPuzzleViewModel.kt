package com.lexicon.presentation.dictationpuzzle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.dictationpuzzle.DictationPuzzleStepResponse
import com.lexicon.interactors.dictationpuzzle.StartDictationPuzzleSessionRequest
import com.lexicon.interactors.dictationpuzzle.StartDictationPuzzleSessionUseCase
import com.lexicon.interactors.dictationpuzzle.SubmitDictationPuzzleAnswerRequest
import com.lexicon.interactors.dictationpuzzle.SubmitDictationPuzzleAnswerUseCase
import com.lexicon.model.training.StepOutcome
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.LetterTile
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.WordResultEntry
import com.lexicon.presentation.common.shuffleIntoTiles
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

private const val CORRECT_ANSWER_ADVANCE_DELAY_MS = 400L
private const val SKIPPED_ANSWER_ADVANCE_DELAY_MS = 700L

class DictationPuzzleViewModel(
    savedStateHandle: SavedStateHandle,
    private val startSessionUseCase: StartDictationPuzzleSessionUseCase,
    private val submitAnswerUseCase: SubmitDictationPuzzleAnswerUseCase,
    private val speechSynthesizer: SpeechSynthesizer,
    private val dispatchers: DispatcherProvider,
    private val lastSessionResultsHolder: LastSessionResultsHolder,
) : ViewModel() {
    private val vocabularyIds = savedStateHandle.trainingVocabularyIds()

    private val _uiState = MutableStateFlow<DictationPuzzleUiState>(DictationPuzzleUiState.Loading)
    val uiState: StateFlow<DictationPuzzleUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<SessionNavigationEvent>()
    val navigationEvents: SharedFlow<SessionNavigationEvent> = _navigationEvents.asSharedFlow()

    private lateinit var sessionId: String
    private var steps: List<DictationPuzzleStepResponse> = emptyList()
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
            val response = startSessionUseCase(StartDictationPuzzleSessionRequest(vocabularyIds = vocabularyIds))
            sessionId = response.sessionId
            steps = response.steps
            if (steps.isEmpty()) {
                _uiState.update { DictationPuzzleUiState.Unavailable }
                return@launch
            }
            openStep(0)
            speakCurrentStep()
        }
    }

    private fun openStep(index: Int) {
        val step = steps.getOrNull(index) ?: return
        _uiState.update {
            DictationPuzzleUiState.Loaded(
                stepIndex = index,
                totalSteps = steps.size,
                stepTiles = shuffleIntoTiles(step.expectedText),
            )
        }
    }

    fun onReplayAudio() {
        viewModelScope.launch(dispatchers.io) { speakCurrentStep() }
    }

    private suspend fun speakCurrentStep() {
        val step = currentStepOrNull() ?: return
        speechSynthesizer.speak(step.expectedText)
    }

    fun onTileSelected(tile: LetterTile) {
        val state = _uiState.value as? DictationPuzzleUiState.Loaded ?: return
        if (!state.isEditable) return
        if (state.placedTiles.any { it.id == tile.id }) return
        updateLoaded { it.copy(placedTiles = it.placedTiles + tile) }
    }

    fun onUndo() {
        val state = _uiState.value as? DictationPuzzleUiState.Loaded ?: return
        if (!state.canUndo) return
        updateLoaded { it.copy(placedTiles = it.placedTiles.dropLast(1)) }
    }

    fun onTipRequested() {
        val state = _uiState.value as? DictationPuzzleUiState.Loaded ?: return
        if (!state.canUseTip) return
        val step = currentStepOrNull() ?: return
        tipsUsedCount++
        updateLoaded { it.copy(tipUsed = true, tipTranslation = step.translationText) }
    }

    fun onCheck() {
        val state = _uiState.value as? DictationPuzzleUiState.Loaded ?: return
        if (!state.canCheck) return
        submitCurrentStep(submittedText = state.builtAnswer, skipped = false)
    }

    fun onSkip() {
        val state = _uiState.value as? DictationPuzzleUiState.Loaded ?: return
        if (!state.canSkip) return
        submitCurrentStep(submittedText = "", skipped = true)
    }

    private fun submitCurrentStep(
        submittedText: String,
        skipped: Boolean,
    ) {
        val step = currentStepOrNull() ?: return
        val state = _uiState.value as? DictationPuzzleUiState.Loaded ?: return
        viewModelScope.launch(dispatchers.io) {
            val response =
                submitAnswerUseCase(
                    SubmitDictationPuzzleAnswerRequest(
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
                updateLoaded { it.copy(answerState = AnswerState.Incorrect(expectedText)) }
            }
            StepOutcome.SKIPPED -> {
                skippedCount++
                step?.let {
                    wordResults +=
                        WordResultEntry(it.expectedText, it.translationText, AnswerState.Skipped(expectedText), tipUsed)
                }
                updateLoaded { it.copy(answerState = AnswerState.Skipped(expectedText)) }
                delay(SKIPPED_ANSWER_ADVANCE_DELAY_MS)
                advanceToNextStep()
            }

            StepOutcome.SEEN -> Unit
        }
    }

    fun onNext() {
        val state = _uiState.value as? DictationPuzzleUiState.Loaded ?: return
        if (!state.awaitingNext) return
        viewModelScope.launch(dispatchers.io) { advanceToNextStep() }
    }

    private suspend fun advanceToNextStep() {
        val state = _uiState.value as? DictationPuzzleUiState.Loaded ?: return
        val nextIndex = state.stepIndex + 1
        if (nextIndex >= steps.size) {
            updateLoaded { it.copy(isSessionComplete = true) }
            lastSessionResultsHolder.wordResults = wordResults.toList()
            _navigationEvents.emit(
                SessionNavigationEvent.SessionComplete(correctCount, incorrectCount, skippedCount, tipsUsedCount),
            )
            return
        }
        openStep(nextIndex)
        speakCurrentStep()
    }

    private fun currentStepOrNull(): DictationPuzzleStepResponse? {
        val state = _uiState.value as? DictationPuzzleUiState.Loaded ?: return null
        return steps.getOrNull(state.stepIndex)
    }

    private inline fun updateLoaded(transform: (DictationPuzzleUiState.Loaded) -> DictationPuzzleUiState.Loaded) {
        _uiState.update { current -> if (current is DictationPuzzleUiState.Loaded) transform(current) else current }
    }
}
