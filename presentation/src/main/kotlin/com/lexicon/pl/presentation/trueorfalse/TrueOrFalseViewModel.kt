package com.lexicon.pl.presentation.trueorfalse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.pl.common.DispatcherProvider
import com.lexicon.pl.interactors.trueorfalse.StartTrueOrFalseSessionRequest
import com.lexicon.pl.interactors.trueorfalse.StartTrueOrFalseSessionUseCase
import com.lexicon.pl.interactors.trueorfalse.SubmitTrueOrFalseAnswerRequest
import com.lexicon.pl.interactors.trueorfalse.SubmitTrueOrFalseAnswerUseCase
import com.lexicon.pl.interactors.trueorfalse.TrueOrFalseStepOutcome
import com.lexicon.pl.interactors.trueorfalse.TrueOrFalseStepResponse
import com.lexicon.pl.presentation.common.AnswerState
import com.lexicon.pl.presentation.common.SessionNavigationEvent
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
class TrueOrFalseViewModel
    @Inject
    constructor(
        private val startSessionUseCase: StartTrueOrFalseSessionUseCase,
        private val submitAnswerUseCase: SubmitTrueOrFalseAnswerUseCase,
        private val dispatchers: DispatcherProvider,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(TrueOrFalseUiState())
        val uiState: StateFlow<TrueOrFalseUiState> = _uiState.asStateFlow()

        private val _navigationEvents = MutableSharedFlow<SessionNavigationEvent>()
        val navigationEvents: SharedFlow<SessionNavigationEvent> = _navigationEvents.asSharedFlow()

        private lateinit var sessionId: String
        private var steps: List<TrueOrFalseStepResponse> = emptyList()
        private var correctCount = 0
        private var incorrectCount = 0
        private var skippedCount = 0

        init {
            startSession()
        }

        private fun startSession() {
            viewModelScope.launch(dispatchers.io) {
                val response = startSessionUseCase(StartTrueOrFalseSessionRequest())
                sessionId = response.sessionId
                steps = response.steps
                openStep(0)
            }
        }

        private fun openStep(index: Int) {
            val step = steps.getOrNull(index) ?: return
            _uiState.update {
                TrueOrFalseUiState(
                    isLoading = false,
                    stepIndex = index,
                    totalSteps = steps.size,
                    word = step.word,
                    displayedTranslation = step.displayedTranslation,
                )
            }
        }

        fun onAnswer(userAnsweredTrue: Boolean) {
            val state = _uiState.value
            if (!state.isEditable) return
            submitCurrentStep(userAnsweredTrue = userAnsweredTrue, skipped = false)
        }

        fun onSkip() {
            val state = _uiState.value
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
                    _uiState.update { it.copy(answerState = AnswerState.CORRECT, userAnsweredTrue = userAnsweredTrue) }
                    delay(CORRECT_ANSWER_ADVANCE_DELAY_MS)
                    advanceToNextStep()
                }
                TrueOrFalseStepOutcome.INCORRECT -> {
                    incorrectCount++
                    _uiState.update { it.copy(answerState = AnswerState.INCORRECT, userAnsweredTrue = userAnsweredTrue) }
                }
                TrueOrFalseStepOutcome.SKIPPED -> {
                    skippedCount++
                    _uiState.update { it.copy(answerState = AnswerState.SKIPPED) }
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
                _navigationEvents.emit(SessionNavigationEvent.SessionComplete(correctCount, incorrectCount, skippedCount))
                return
            }
            openStep(nextIndex)
        }

        private fun currentStepOrNull(): TrueOrFalseStepResponse? = steps.getOrNull(_uiState.value.stepIndex)
    }
