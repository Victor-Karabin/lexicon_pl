package com.lexicon.pl.presentation.imagetest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.pl.common.DispatcherProvider
import com.lexicon.pl.interactors.imagetest.ImageTestStepOutcome
import com.lexicon.pl.interactors.imagetest.ImageTestStepResponse
import com.lexicon.pl.interactors.imagetest.StartImageTestSessionRequest
import com.lexicon.pl.interactors.imagetest.StartImageTestSessionUseCase
import com.lexicon.pl.interactors.imagetest.SubmitImageTestAnswerRequest
import com.lexicon.pl.interactors.imagetest.SubmitImageTestAnswerUseCase
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
class ImageTestViewModel
    @Inject
    constructor(
        private val startSessionUseCase: StartImageTestSessionUseCase,
        private val submitAnswerUseCase: SubmitImageTestAnswerUseCase,
        private val dispatchers: DispatcherProvider,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ImageTestUiState())
        val uiState: StateFlow<ImageTestUiState> = _uiState.asStateFlow()

        private val _navigationEvents = MutableSharedFlow<SessionNavigationEvent>()
        val navigationEvents: SharedFlow<SessionNavigationEvent> = _navigationEvents.asSharedFlow()

        private lateinit var sessionId: String
        private var steps: List<ImageTestStepResponse> = emptyList()
        private var correctCount = 0
        private var incorrectCount = 0
        private var skippedCount = 0

        init {
            startSession()
        }

        private fun startSession() {
            viewModelScope.launch(dispatchers.io) {
                val response = startSessionUseCase(StartImageTestSessionRequest())
                sessionId = response.sessionId
                steps = response.steps
                openStep(0)
            }
        }

        private fun openStep(index: Int) {
            val step = steps.getOrNull(index) ?: return
            _uiState.update {
                ImageTestUiState(
                    isLoading = false,
                    stepIndex = index,
                    totalSteps = steps.size,
                    imageUrl = step.imageUrl,
                    clueText = step.clueText,
                    options = step.options,
                )
            }
        }

        fun onOptionSelected(option: String) {
            if (!_uiState.value.isEditable) return
            _uiState.update { it.copy(selectedOption = option) }
        }

        fun onCheck() {
            val state = _uiState.value
            if (!state.canCheck) return
            submitCurrentStep(selectedOption = state.selectedOption, skipped = false)
        }

        fun onSkip() {
            val state = _uiState.value
            if (!state.canSkip) return
            submitCurrentStep(selectedOption = null, skipped = true)
        }

        private fun submitCurrentStep(
            selectedOption: String?,
            skipped: Boolean,
        ) {
            val step = currentStepOrNull() ?: return
            viewModelScope.launch(dispatchers.io) {
                val response =
                    submitAnswerUseCase(
                        SubmitImageTestAnswerRequest(
                            sessionId = sessionId,
                            stepIndex = step.stepIndex,
                            vocabularyItemId = step.vocabularyItemId,
                            correctOption = step.correctOption,
                            selectedOption = selectedOption,
                            skipped = skipped,
                        ),
                    )
                applyOutcome(response.outcome, response.correctOption)
            }
        }

        private suspend fun applyOutcome(
            outcome: ImageTestStepOutcome,
            correctOption: String,
        ) {
            when (outcome) {
                ImageTestStepOutcome.CORRECT -> {
                    correctCount++
                    _uiState.update { it.copy(answerState = AnswerState.CORRECT, correctOption = correctOption) }
                    delay(CORRECT_ANSWER_ADVANCE_DELAY_MS)
                    advanceToNextStep()
                }
                ImageTestStepOutcome.INCORRECT -> {
                    incorrectCount++
                    _uiState.update { it.copy(answerState = AnswerState.INCORRECT, correctOption = correctOption) }
                }
                ImageTestStepOutcome.SKIPPED -> {
                    skippedCount++
                    _uiState.update { it.copy(answerState = AnswerState.SKIPPED, correctOption = correctOption) }
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

        private fun currentStepOrNull(): ImageTestStepResponse? = steps.getOrNull(_uiState.value.stepIndex)
    }
