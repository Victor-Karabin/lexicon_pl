package com.lexicon.presentation.puzzle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.puzzle.PuzzleStepOutcome
import com.lexicon.interactors.puzzle.PuzzleStepResponse
import com.lexicon.interactors.puzzle.StartPuzzleSessionRequest
import com.lexicon.interactors.puzzle.StartPuzzleSessionUseCase
import com.lexicon.interactors.puzzle.SubmitPuzzleAnswerRequest
import com.lexicon.interactors.puzzle.SubmitPuzzleAnswerUseCase
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.LetterTile
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.WordResultEntry
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
class PuzzleViewModel
    @Inject
    constructor(
        private val startSessionUseCase: StartPuzzleSessionUseCase,
        private val submitAnswerUseCase: SubmitPuzzleAnswerUseCase,
        private val dispatchers: DispatcherProvider,
        private val lastSessionResultsHolder: LastSessionResultsHolder,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<PuzzleUiState>(PuzzleUiState.Loading)
        val uiState: StateFlow<PuzzleUiState> = _uiState.asStateFlow()

        private val _navigationEvents = MutableSharedFlow<SessionNavigationEvent>()
        val navigationEvents: SharedFlow<SessionNavigationEvent> = _navigationEvents.asSharedFlow()

        private lateinit var sessionId: String
        private var steps: List<PuzzleStepResponse> = emptyList()
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
                val response = startSessionUseCase(StartPuzzleSessionRequest())
                sessionId = response.sessionId
                steps = response.steps
                openStep(0)
            }
        }

        private fun openStep(index: Int) {
            val step = steps.getOrNull(index) ?: return
            _uiState.update {
                PuzzleUiState.Loaded(
                    stepIndex = index,
                    totalSteps = steps.size,
                    imageUrl = step.imageUrl,
                    clueText = step.clueText,
                    stepTiles = shuffleIntoTiles(step.expectedText),
                )
            }
        }

        fun onTileSelected(tile: LetterTile) {
            val state = _uiState.value as? PuzzleUiState.Loaded ?: return
            if (!state.isEditable) return
            if (state.placedTiles.any { it.id == tile.id }) return
            updateLoaded { it.copy(placedTiles = it.placedTiles + tile) }
        }

        fun onUndo() {
            val state = _uiState.value as? PuzzleUiState.Loaded ?: return
            if (!state.canUndo) return
            updateLoaded { it.copy(placedTiles = it.placedTiles.dropLast(1)) }
        }

        fun onTipRequested() {
            val state = _uiState.value as? PuzzleUiState.Loaded ?: return
            if (!state.canUseTip) return
            val step = currentStepOrNull() ?: return
            tipsUsedCount++
            updateLoaded { it.copy(tipUsed = true, tipTranslation = step.clueText) }
        }

        fun onCheck() {
            val state = _uiState.value as? PuzzleUiState.Loaded ?: return
            if (!state.canCheck) return
            submitCurrentStep(submittedText = state.builtAnswer, skipped = false)
        }

        fun onSkip() {
            val state = _uiState.value as? PuzzleUiState.Loaded ?: return
            if (!state.canSkip) return
            submitCurrentStep(submittedText = "", skipped = true)
        }

        private fun submitCurrentStep(
            submittedText: String,
            skipped: Boolean,
        ) {
            val step = currentStepOrNull() ?: return
            val state = _uiState.value as? PuzzleUiState.Loaded ?: return
            viewModelScope.launch(dispatchers.io) {
                val response =
                    submitAnswerUseCase(
                        SubmitPuzzleAnswerRequest(
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
            outcome: PuzzleStepOutcome,
            expectedText: String,
            tipUsed: Boolean,
        ) {
            val step = currentStepOrNull()
            when (outcome) {
                PuzzleStepOutcome.CORRECT -> {
                    correctCount++
                    step?.let {
                        wordResults += WordResultEntry(it.expectedText, it.clueText, AnswerState.Correct, tipUsed)
                    }
                    updateLoaded { it.copy(answerState = AnswerState.Correct) }
                    delay(CORRECT_ANSWER_ADVANCE_DELAY_MS)
                    advanceToNextStep()
                }

                PuzzleStepOutcome.INCORRECT -> {
                    incorrectCount++
                    step?.let {
                        wordResults += WordResultEntry(it.expectedText, it.clueText, AnswerState.Incorrect(expectedText), tipUsed)
                    }
                    updateLoaded { it.copy(answerState = AnswerState.Incorrect(expectedText)) }
                }

                PuzzleStepOutcome.SKIPPED -> {
                    skippedCount++
                    step?.let {
                        wordResults += WordResultEntry(it.expectedText, it.clueText, AnswerState.Skipped(expectedText), tipUsed)
                    }
                    updateLoaded { it.copy(answerState = AnswerState.Skipped(expectedText)) }
                }
            }
        }

        fun onNext() {
            val state = _uiState.value as? PuzzleUiState.Loaded ?: return
            if (!state.awaitingNext) return
            viewModelScope.launch(dispatchers.io) { advanceToNextStep() }
        }

        private suspend fun advanceToNextStep() {
            val state = _uiState.value as? PuzzleUiState.Loaded ?: return
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
        }

        private fun currentStepOrNull(): PuzzleStepResponse? {
            val state = _uiState.value as? PuzzleUiState.Loaded ?: return null
            return steps.getOrNull(state.stepIndex)
        }

        private inline fun updateLoaded(transform: (PuzzleUiState.Loaded) -> PuzzleUiState.Loaded) {
            _uiState.update { current -> if (current is PuzzleUiState.Loaded) transform(current) else current }
        }
    }
