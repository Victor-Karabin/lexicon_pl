package com.lexicon.presentation.imagetest

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.imagetest.ImageTestStepOutcome
import com.lexicon.interactors.imagetest.ImageTestStepResponse
import com.lexicon.interactors.imagetest.StartImageTestSessionRequest
import com.lexicon.interactors.imagetest.StartImageTestSessionUseCase
import com.lexicon.interactors.imagetest.SubmitImageTestAnswerRequest
import com.lexicon.interactors.imagetest.SubmitImageTestAnswerUseCase
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.trainingVocabularyIds
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
        savedStateHandle: SavedStateHandle,
        private val startSessionUseCase: StartImageTestSessionUseCase,
        private val submitAnswerUseCase: SubmitImageTestAnswerUseCase,
        private val dispatchers: DispatcherProvider,
    ) : ViewModel() {
        private val vocabularyIds = savedStateHandle.trainingVocabularyIds()

        private val _uiState = MutableStateFlow<ImageTestUiState>(ImageTestUiState.Loading)
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
                val response = startSessionUseCase(StartImageTestSessionRequest(vocabularyIds = vocabularyIds))
                sessionId = response.sessionId
                steps = response.steps
                openStep(0)
            }
        }

        private fun openStep(index: Int) {
            val step = steps.getOrNull(index) ?: return
            _uiState.update {
                ImageTestUiState.Loaded(
                    stepIndex = index,
                    totalSteps = steps.size,
                    imageUrl = step.imageUrl,
                    clueText = step.clueText,
                    options = step.options,
                )
            }
        }

        fun onOptionSelected(option: String) {
            val state = _uiState.value as? ImageTestUiState.Loaded ?: return
            if (!state.isEditable) return
            updateLoaded { it.copy(selectedOption = option) }
        }

        fun onCheck() {
            val state = _uiState.value as? ImageTestUiState.Loaded ?: return
            if (!state.canCheck) return
            submitCurrentStep(selectedOption = state.selectedOption, skipped = false)
        }

        fun onSkip() {
            val state = _uiState.value as? ImageTestUiState.Loaded ?: return
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
                    updateLoaded { it.copy(answerState = AnswerState.Correct, correctOption = correctOption) }
                    delay(CORRECT_ANSWER_ADVANCE_DELAY_MS)
                    advanceToNextStep()
                }
                ImageTestStepOutcome.INCORRECT -> {
                    incorrectCount++
                    updateLoaded { it.copy(answerState = AnswerState.Incorrect(), correctOption = correctOption) }
                }
                ImageTestStepOutcome.SKIPPED -> {
                    skippedCount++
                    updateLoaded { it.copy(answerState = AnswerState.Skipped(), correctOption = correctOption) }
                }
            }
        }

        fun onNext() {
            val state = _uiState.value as? ImageTestUiState.Loaded ?: return
            if (!state.awaitingNext) return
            viewModelScope.launch(dispatchers.io) { advanceToNextStep() }
        }

        private suspend fun advanceToNextStep() {
            val state = _uiState.value as? ImageTestUiState.Loaded ?: return
            val nextIndex = state.stepIndex + 1
            if (nextIndex >= steps.size) {
                updateLoaded { it.copy(isSessionComplete = true) }
                _navigationEvents.emit(SessionNavigationEvent.SessionComplete(correctCount, incorrectCount, skippedCount))
                return
            }
            openStep(nextIndex)
        }

        private fun currentStepOrNull(): ImageTestStepResponse? {
            val state = _uiState.value as? ImageTestUiState.Loaded ?: return null
            return steps.getOrNull(state.stepIndex)
        }

        private inline fun updateLoaded(transform: (ImageTestUiState.Loaded) -> ImageTestUiState.Loaded) {
            _uiState.update { current -> if (current is ImageTestUiState.Loaded) transform(current) else current }
        }
    }
