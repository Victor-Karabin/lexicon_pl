package com.lexicon.presentation.wordbuilder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.wordbuilder.StartWordBuilderSessionRequest
import com.lexicon.interactors.wordbuilder.StartWordBuilderSessionUseCase
import com.lexicon.interactors.wordbuilder.SubmitWordBuilderAnswerRequest
import com.lexicon.interactors.wordbuilder.SubmitWordBuilderAnswerUseCase
import com.lexicon.interactors.wordbuilder.WordBuilderStepOutcome
import com.lexicon.interactors.wordbuilder.WordBuilderStepResponse
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LetterTile
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.shuffleIntoTiles
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
class WordBuilderViewModel
    @Inject
    constructor(
        private val startSessionUseCase: StartWordBuilderSessionUseCase,
        private val submitAnswerUseCase: SubmitWordBuilderAnswerUseCase,
        private val dispatchers: DispatcherProvider,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<WordBuilderUiState>(WordBuilderUiState.Loading)
        val uiState: StateFlow<WordBuilderUiState> = _uiState.asStateFlow()

        private val _navigationEvents = MutableSharedFlow<SessionNavigationEvent>()
        val navigationEvents: SharedFlow<SessionNavigationEvent> = _navigationEvents.asSharedFlow()

        private lateinit var sessionId: String
        private var steps: List<WordBuilderStepResponse> = emptyList()
        private var correctCount = 0
        private var incorrectCount = 0
        private var skippedCount = 0
        private var tipsUsedCount = 0

        init {
            startSession()
        }

        private fun startSession() {
            viewModelScope.launch(dispatchers.io) {
                val response = startSessionUseCase(StartWordBuilderSessionRequest())
                sessionId = response.sessionId
                steps = response.steps
                openStep(0)
            }
        }

        private fun openStep(index: Int) {
            val step = steps.getOrNull(index) ?: return
            _uiState.update {
                WordBuilderUiState.Loaded(
                    stepIndex = index,
                    totalSteps = steps.size,
                    clueText = step.clueText,
                    stepTiles = shuffleIntoTiles(step.expectedText),
                )
            }
        }

        fun onTileSelected(tile: LetterTile) {
            val state = _uiState.value as? WordBuilderUiState.Loaded ?: return
            if (!state.isEditable) return
            if (state.placedTiles.any { it.id == tile.id }) return
            updateLoaded { it.copy(placedTiles = it.placedTiles + tile) }
        }

        fun onAnswerFieldCleared() {
            val state = _uiState.value as? WordBuilderUiState.Loaded ?: return
            if (!state.isEditable) return
            updateLoaded { it.copy(placedTiles = emptyList()) }
        }

        fun onTipRequested() {
            val state = _uiState.value as? WordBuilderUiState.Loaded ?: return
            if (!state.canUseTip) return
            val step = currentStepOrNull() ?: return
            tipsUsedCount++
            updateLoaded { it.copy(tipUsed = true, tipTranslation = step.expectedText) }
        }

        fun onCheck() {
            val state = _uiState.value as? WordBuilderUiState.Loaded ?: return
            if (!state.canCheck) return
            submitCurrentStep(submittedText = state.builtAnswer, skipped = false)
        }

        fun onSkip() {
            val state = _uiState.value as? WordBuilderUiState.Loaded ?: return
            if (!state.canSkip) return
            submitCurrentStep(submittedText = "", skipped = true)
        }

        private fun submitCurrentStep(
            submittedText: String,
            skipped: Boolean,
        ) {
            val step = currentStepOrNull() ?: return
            val state = _uiState.value as? WordBuilderUiState.Loaded ?: return
            viewModelScope.launch(dispatchers.io) {
                val response =
                    submitAnswerUseCase(
                        SubmitWordBuilderAnswerRequest(
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

        private suspend fun applyOutcome(
            outcome: WordBuilderStepOutcome,
            expectedText: String,
        ) {
            when (outcome) {
                WordBuilderStepOutcome.CORRECT -> {
                    correctCount++
                    updateLoaded { it.copy(answerState = AnswerState.Correct) }
                    delay(CORRECT_ANSWER_ADVANCE_DELAY_MS)
                    advanceToNextStep()
                }
                WordBuilderStepOutcome.INCORRECT -> {
                    incorrectCount++
                    updateLoaded { it.copy(answerState = AnswerState.Incorrect(expectedText)) }
                }
                WordBuilderStepOutcome.SKIPPED -> {
                    skippedCount++
                    updateLoaded { it.copy(answerState = AnswerState.Skipped(expectedText)) }
                }
            }
        }

        /** Called from the UI's "Next" button after an Incorrect/Skipped step. */
        fun onNext() {
            val state = _uiState.value as? WordBuilderUiState.Loaded ?: return
            if (!state.awaitingNext) return
            viewModelScope.launch(dispatchers.io) { advanceToNextStep() }
        }

        private suspend fun advanceToNextStep() {
            val state = _uiState.value as? WordBuilderUiState.Loaded ?: return
            val nextIndex = state.stepIndex + 1
            if (nextIndex >= steps.size) {
                updateLoaded { it.copy(isSessionComplete = true) }
                _navigationEvents.emit(
                    SessionNavigationEvent.SessionComplete(correctCount, incorrectCount, skippedCount, tipsUsedCount),
                )
                return
            }
            openStep(nextIndex)
        }

        private fun currentStepOrNull(): WordBuilderStepResponse? {
            val state = _uiState.value as? WordBuilderUiState.Loaded ?: return null
            return steps.getOrNull(state.stepIndex)
        }

        private inline fun updateLoaded(transform: (WordBuilderUiState.Loaded) -> WordBuilderUiState.Loaded) {
            _uiState.update { current -> if (current is WordBuilderUiState.Loaded) transform(current) else current }
        }
    }
