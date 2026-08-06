package com.lexicon.presentation.wordmatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.wordmatch.StartWordMatchSessionRequest
import com.lexicon.interactors.wordmatch.StartWordMatchSessionUseCase
import com.lexicon.interactors.wordmatch.SubmitWordMatchStepResultRequest
import com.lexicon.interactors.wordmatch.SubmitWordMatchStepResultUseCase
import com.lexicon.interactors.wordmatch.WordMatchStepOutcome
import com.lexicon.interactors.wordmatch.WordMatchStepResponse
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.SessionNavigationEvent
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
class WordMatchViewModel
    @Inject
    constructor(
        private val startSessionUseCase: StartWordMatchSessionUseCase,
        private val submitStepResultUseCase: SubmitWordMatchStepResultUseCase,
        private val dispatchers: DispatcherProvider,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<WordMatchUiState>(WordMatchUiState.Loading)
        val uiState: StateFlow<WordMatchUiState> = _uiState.asStateFlow()

        private val _navigationEvents = MutableSharedFlow<SessionNavigationEvent>()
        val navigationEvents: SharedFlow<SessionNavigationEvent> = _navigationEvents.asSharedFlow()

        private lateinit var sessionId: String
        private var steps: List<WordMatchStepResponse> = emptyList()
        private var correctCount = 0
        private var incorrectCount = 0
        private var skippedCount = 0

        init {
            startSession()
        }

        private fun startSession() {
            viewModelScope.launch(dispatchers.io) {
                val response = startSessionUseCase(StartWordMatchSessionRequest())
                sessionId = response.sessionId
                steps = response.steps
                openStep(0)
            }
        }

        private fun openStep(index: Int) {
            val step = steps.getOrNull(index) ?: return
            _uiState.update {
                WordMatchUiState.Loaded(
                    stepIndex = index,
                    totalSteps = steps.size,
                    leftColumn = step.pairs.map { WordMatchColumnItem(it.vocabularyItemId, it.word) }.shuffled(),
                    rightColumn = step.pairs.map { WordMatchColumnItem(it.vocabularyItemId, it.translation) }.shuffled(),
                )
            }
        }

        fun onLeftSelected(vocabularyItemId: Long) {
            val state = _uiState.value as? WordMatchUiState.Loaded ?: return
            if (!state.isInteractive || state.matchedIds.contains(vocabularyItemId)) return
            updateLoaded { it.copy(selectedLeftId = vocabularyItemId, incorrectFlashIds = emptySet()) }
            tryValidateSelection()
        }

        fun onRightSelected(vocabularyItemId: Long) {
            val state = _uiState.value as? WordMatchUiState.Loaded ?: return
            if (!state.isInteractive || state.matchedIds.contains(vocabularyItemId)) return
            updateLoaded { it.copy(selectedRightId = vocabularyItemId, incorrectFlashIds = emptySet()) }
            tryValidateSelection()
        }

        private fun tryValidateSelection() {
            val state = _uiState.value as? WordMatchUiState.Loaded ?: return
            val leftId = state.selectedLeftId ?: return
            val rightId = state.selectedRightId ?: return

            if (leftId == rightId) {
                val matched = state.matchedIds + leftId
                updateLoaded { it.copy(matchedIds = matched, selectedLeftId = null, selectedRightId = null) }
                if (matched.size == state.leftColumn.size) {
                    viewModelScope.launch(dispatchers.io) { completeStep() }
                }
            } else {
                // Left as incorrect (red) until the user's next selection attempt clears it, rather
                // than auto-resetting after a fixed delay.
                updateLoaded {
                    it.copy(
                        incorrectAttempts = it.incorrectAttempts + 1,
                        incorrectFlashIds = setOf(leftId, rightId),
                        selectedLeftId = null,
                        selectedRightId = null,
                    )
                }
            }
        }

        private suspend fun completeStep() {
            val step = currentStepOrNull() ?: return
            val state = _uiState.value as? WordMatchUiState.Loaded ?: return
            val response =
                submitStepResultUseCase(
                    SubmitWordMatchStepResultRequest(
                        sessionId = sessionId,
                        stepIndex = step.stepIndex,
                        vocabularyItemIds = step.pairs.map { it.vocabularyItemId },
                        incorrectAttempts = state.incorrectAttempts,
                        skipped = false,
                    ),
                )
            when (response.outcome) {
                WordMatchStepOutcome.CORRECT -> correctCount++
                WordMatchStepOutcome.INCORRECT -> incorrectCount++
                WordMatchStepOutcome.SKIPPED -> Unit
            }
            delay(CORRECT_ANSWER_ADVANCE_DELAY_MS)
            advanceToNextStep()
        }

        fun onSkip() {
            val state = _uiState.value as? WordMatchUiState.Loaded ?: return
            if (!state.canSkip) return
            val step = currentStepOrNull() ?: return
            viewModelScope.launch(dispatchers.io) {
                submitStepResultUseCase(
                    SubmitWordMatchStepResultRequest(
                        sessionId = sessionId,
                        stepIndex = step.stepIndex,
                        vocabularyItemIds = step.pairs.map { it.vocabularyItemId },
                        incorrectAttempts = state.incorrectAttempts,
                        skipped = true,
                    ),
                )
                skippedCount++
                updateLoaded {
                    it.copy(
                        matchedIds = step.pairs.map { pair -> pair.vocabularyItemId }.toSet(),
                        answerState = AnswerState.Skipped(),
                    )
                }
            }
        }

        /** Called from the UI's "Next" button after a Skipped step. */
        fun onNext() {
            val state = _uiState.value as? WordMatchUiState.Loaded ?: return
            if (!state.awaitingNext) return
            viewModelScope.launch(dispatchers.io) { advanceToNextStep() }
        }

        private suspend fun advanceToNextStep() {
            val state = _uiState.value as? WordMatchUiState.Loaded ?: return
            val nextIndex = state.stepIndex + 1
            if (nextIndex >= steps.size) {
                updateLoaded { it.copy(isSessionComplete = true) }
                _navigationEvents.emit(SessionNavigationEvent.SessionComplete(correctCount, incorrectCount, skippedCount))
                return
            }
            openStep(nextIndex)
        }

        private fun currentStepOrNull(): WordMatchStepResponse? {
            val state = _uiState.value as? WordMatchUiState.Loaded ?: return null
            return steps.getOrNull(state.stepIndex)
        }

        private inline fun updateLoaded(transform: (WordMatchUiState.Loaded) -> WordMatchUiState.Loaded) {
            _uiState.update { current -> if (current is WordMatchUiState.Loaded) transform(current) else current }
        }
    }
