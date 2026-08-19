package com.lexicon.presentation.trueorfalse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.trueorfalse.StartTrueOrFalseSessionRequest
import com.lexicon.interactors.trueorfalse.StartTrueOrFalseSessionUseCase
import com.lexicon.interactors.trueorfalse.SubmitTrueOrFalseAnswerRequest
import com.lexicon.interactors.trueorfalse.SubmitTrueOrFalseAnswerUseCase
import com.lexicon.interactors.trueorfalse.TrueOrFalseStepResponse
import com.lexicon.model.training.StepOutcome
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.WordResultEntry
import com.lexicon.presentation.common.trainingVocabularyIds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TrueOrFalseViewModel(
    savedStateHandle: SavedStateHandle,
    private val startSessionUseCase: StartTrueOrFalseSessionUseCase,
    private val submitAnswerUseCase: SubmitTrueOrFalseAnswerUseCase,
    private val dispatchers: DispatcherProvider,
    private val lastSessionResultsHolder: LastSessionResultsHolder,
) : ViewModel() {
    private val vocabularyIds = savedStateHandle.trainingVocabularyIds()

    private val _uiState = MutableStateFlow<TrueOrFalseUiState>(TrueOrFalseUiState.Loading)
    val uiState: StateFlow<TrueOrFalseUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<SessionNavigationEvent>()
    val navigationEvents: SharedFlow<SessionNavigationEvent> = _navigationEvents.asSharedFlow()

    private lateinit var sessionId: String
    private var steps: List<TrueOrFalseStepResponse> = emptyList()
    private var correctCount = 0
    private var incorrectCount = 0
    private var skippedCount = 0
    private var timerJob: Job? = null
    private val wordResults = mutableListOf<WordResultEntry>()

    init {
        startSession()
    }

    private fun startSession() {
        viewModelScope.launch(dispatchers.io) {
            val response = startSessionUseCase(StartTrueOrFalseSessionRequest(vocabularyIds = vocabularyIds))
            sessionId = response.sessionId
            steps = response.steps
            openStep(0)
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            var remaining = TRUE_OR_FALSE_TIME_LIMIT_SECONDS
            while (remaining > 0) {
                delay(1_000)
                remaining--
                updateLoaded { it.copy(timeRemainingSeconds = remaining) }
            }
            completeSession()
        }
    }

    private fun openStep(index: Int) {
        val step = steps.getOrNull(index) ?: return
        _uiState.update {
            TrueOrFalseUiState.Loaded(
                stepIndex = index,
                timeRemainingSeconds = (it as? TrueOrFalseUiState.Loaded)?.timeRemainingSeconds ?: TRUE_OR_FALSE_TIME_LIMIT_SECONDS,
                word = step.word,
                displayedTranslation = step.displayedTranslation,
            )
        }
    }

    fun onAnswer(userAnsweredTrue: Boolean) {
        val state = _uiState.value as? TrueOrFalseUiState.Loaded ?: return
        if (!state.isEditable) return
        val step = currentStepOrNull() ?: return
        updateLoaded { it.copy(isSubmitting = true) }
        viewModelScope.launch(dispatchers.io) {
            val response =
                submitAnswerUseCase(
                    SubmitTrueOrFalseAnswerRequest(
                        sessionId = sessionId,
                        stepIndex = step.stepIndex,
                        vocabularyItemId = step.vocabularyItemId,
                        isDisplayedTranslationCorrect = step.isDisplayedTranslationCorrect,
                        userAnsweredTrue = userAnsweredTrue,
                    ),
                )
            applyOutcome(response.outcome, step)
        }
    }

    private suspend fun applyOutcome(
        outcome: StepOutcome,
        step: TrueOrFalseStepResponse,
    ) {
        when (outcome) {
            StepOutcome.CORRECT -> {
                correctCount++
                wordResults += WordResultEntry(step.word, step.displayedTranslation, AnswerState.Correct)
            }
            StepOutcome.INCORRECT -> {
                incorrectCount++
                wordResults += WordResultEntry(step.word, step.displayedTranslation, AnswerState.Incorrect())
            }

            StepOutcome.SKIPPED -> {
                skippedCount++
                wordResults += WordResultEntry(step.word, step.displayedTranslation, AnswerState.Skipped())
            }

            StepOutcome.SEEN -> Unit
        }
        advanceToNextStep()
    }

    private suspend fun advanceToNextStep() {
        val state = _uiState.value as? TrueOrFalseUiState.Loaded ?: return
        if (state.isSessionComplete) return
        val nextIndex = state.stepIndex + 1
        if (nextIndex >= steps.size) {
            completeSession()
            return
        }
        openStep(nextIndex)
    }

    private suspend fun completeSession() {
        val state = _uiState.value as? TrueOrFalseUiState.Loaded ?: return
        if (state.isSessionComplete) return
        updateLoaded { it.copy(isSessionComplete = true) }
        lastSessionResultsHolder.wordResults = wordResults.toList()
        _navigationEvents.emit(SessionNavigationEvent.SessionComplete(correctCount, incorrectCount, skipped = skippedCount))
        timerJob?.cancel()
    }

    private fun currentStepOrNull(): TrueOrFalseStepResponse? {
        val state = _uiState.value as? TrueOrFalseUiState.Loaded ?: return null
        return steps.getOrNull(state.stepIndex)
    }

    private inline fun updateLoaded(transform: (TrueOrFalseUiState.Loaded) -> TrueOrFalseUiState.Loaded) {
        _uiState.update { current -> if (current is TrueOrFalseUiState.Loaded) transform(current) else current }
    }

    override fun onCleared() {
        timerJob?.cancel()
    }
}
