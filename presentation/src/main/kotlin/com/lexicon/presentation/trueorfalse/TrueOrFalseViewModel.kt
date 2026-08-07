package com.lexicon.presentation.trueorfalse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.trueorfalse.StartTrueOrFalseSessionRequest
import com.lexicon.interactors.trueorfalse.StartTrueOrFalseSessionUseCase
import com.lexicon.interactors.trueorfalse.SubmitTrueOrFalseAnswerRequest
import com.lexicon.interactors.trueorfalse.SubmitTrueOrFalseAnswerUseCase
import com.lexicon.interactors.trueorfalse.TrueOrFalseStepOutcome
import com.lexicon.interactors.trueorfalse.TrueOrFalseStepResponse
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.SessionNavigationEvent
import dagger.hilt.android.lifecycle.HiltViewModel
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
import javax.inject.Inject

private const val CORRECT_ANSWER_ADVANCE_DELAY_MS = 400L

@HiltViewModel
class TrueOrFalseViewModel
    @Inject
    constructor(
        private val startSessionUseCase: StartTrueOrFalseSessionUseCase,
        private val submitAnswerUseCase: SubmitTrueOrFalseAnswerUseCase,
        private val dispatchers: DispatcherProvider,
    ) : ViewModel() {
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

        init {
            startSession()
        }

        private fun startSession() {
            viewModelScope.launch(dispatchers.io) {
                val response = startSessionUseCase(StartTrueOrFalseSessionRequest())
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
            submitCurrentStep(userAnsweredTrue = userAnsweredTrue, skipped = false)
        }

        fun onSkip() {
            val state = _uiState.value as? TrueOrFalseUiState.Loaded ?: return
            if (!state.canSkip) return
            submitCurrentStep(userAnsweredTrue = null, skipped = true)
        }

        private fun submitCurrentStep(
            userAnsweredTrue: Boolean?,
            skipped: Boolean,
        ) {
            val step = currentStepOrNull() ?: return
            viewModelScope.launch(dispatchers.io) {
                val response =
                    submitAnswerUseCase(
                        SubmitTrueOrFalseAnswerRequest(
                            sessionId = sessionId,
                            stepIndex = step.stepIndex,
                            vocabularyItemId = step.vocabularyItemId,
                            isDisplayedTranslationCorrect = step.isDisplayedTranslationCorrect,
                            userAnsweredTrue = userAnsweredTrue,
                            skipped = skipped,
                        ),
                    )
                applyOutcome(response.outcome, userAnsweredTrue)
            }
        }

        private suspend fun applyOutcome(
            outcome: TrueOrFalseStepOutcome,
            userAnsweredTrue: Boolean?,
        ) {
            when (outcome) {
                TrueOrFalseStepOutcome.CORRECT -> {
                    correctCount++
                    updateLoaded { it.copy(answerState = AnswerState.Correct, userAnsweredTrue = userAnsweredTrue) }
                    delay(CORRECT_ANSWER_ADVANCE_DELAY_MS)
                    advanceToNextStep()
                }
                TrueOrFalseStepOutcome.INCORRECT -> {
                    incorrectCount++
                    updateLoaded { it.copy(answerState = AnswerState.Incorrect(), userAnsweredTrue = userAnsweredTrue) }
                }
                TrueOrFalseStepOutcome.SKIPPED -> {
                    skippedCount++
                    updateLoaded { it.copy(answerState = AnswerState.Skipped()) }
                }
            }
        }

        /** Called from the UI's "Next" button after an Incorrect/Skipped step. */
        fun onNext() {
            val state = _uiState.value as? TrueOrFalseUiState.Loaded ?: return
            if (!state.awaitingNext) return
            viewModelScope.launch(dispatchers.io) { advanceToNextStep() }
        }

        private suspend fun advanceToNextStep() {
            val nextIndex = (_uiState.value as? TrueOrFalseUiState.Loaded)?.stepIndex?.plus(1) ?: return
            if (nextIndex >= steps.size) {
                // Ran out of fetched items before the timer expired.
                completeSession()
                return
            }
            openStep(nextIndex)
        }

        private suspend fun completeSession() {
            val state = _uiState.value as? TrueOrFalseUiState.Loaded ?: return
            if (state.isSessionComplete) return
            timerJob?.cancel()
            updateLoaded { it.copy(isSessionComplete = true) }
            _navigationEvents.emit(SessionNavigationEvent.SessionComplete(correctCount, incorrectCount, skippedCount))
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
