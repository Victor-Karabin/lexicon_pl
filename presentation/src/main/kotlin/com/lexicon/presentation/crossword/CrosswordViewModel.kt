package com.lexicon.presentation.crossword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.crossword.CrosswordWordOutcome
import com.lexicon.interactors.crossword.CrosswordWordSubmission
import com.lexicon.interactors.crossword.StartCrosswordSessionRequest
import com.lexicon.interactors.crossword.StartCrosswordSessionUseCase
import com.lexicon.interactors.crossword.SubmitCrosswordRequest
import com.lexicon.interactors.crossword.SubmitCrosswordUseCase
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.WordResultEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CrosswordViewModel
    @Inject
    constructor(
        private val startSessionUseCase: StartCrosswordSessionUseCase,
        private val submitCrosswordUseCase: SubmitCrosswordUseCase,
        private val dispatchers: DispatcherProvider,
        private val lastSessionResultsHolder: LastSessionResultsHolder,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<CrosswordUiState>(CrosswordUiState.Loading)
        val uiState: StateFlow<CrosswordUiState> = _uiState.asStateFlow()

        private val _navigationEvents = MutableSharedFlow<SessionNavigationEvent>()
        val navigationEvents: SharedFlow<SessionNavigationEvent> = _navigationEvents.asSharedFlow()

        private lateinit var sessionId: String

        /** Held between Check and Continue so the user can review the marked grid before navigating. */
        private var pendingResult: SessionNavigationEvent.SessionComplete? = null

        init {
            startSession()
        }

        private fun startSession() {
            viewModelScope.launch(dispatchers.io) {
                val response = startSessionUseCase(StartCrosswordSessionRequest())
                sessionId = response.sessionId
                val words = response.words.map { placement ->
                    CrosswordWordUi(
                        vocabularyItemId = placement.vocabularyItemId,
                        row = placement.row,
                        col = placement.col,
                        direction = placement.direction,
                        length = placement.expectedText.length,
                        imageUrl = placement.imageUrl,
                        clueText = placement.clueText,
                        expectedText = placement.expectedText,
                    )
                }
                _uiState.update {
                    CrosswordUiState.Loaded(
                        words = words,
                        cells = words.flatMap { it.occupiedCells() }.associateWith { CrosswordCellState() },
                        rowCount = response.rowCount,
                        colCount = response.colCount,
                        selectedWordId = words.firstOrNull()?.vocabularyItemId,
                    )
                }
            }
        }

        fun onClueSelected(vocabularyItemId: Long) {
            updateLoaded { state ->
                if (!state.isEditable) state else state.copy(selectedWordId = vocabularyItemId)
            }
        }

        fun onCellSelected(cell: CrosswordCell) {
            updateLoaded { state ->
                if (!state.isEditable) return@updateLoaded state
                // A cell can belong to two crossing words; prefer keeping the current selection if it covers the cell.
                val current = state.selectedWord
                if (current != null && current.occupiedCells().contains(cell)) return@updateLoaded state
                val owner = state.words.firstOrNull { it.occupiedCells().contains(cell) } ?: return@updateLoaded state
                state.copy(selectedWordId = owner.vocabularyItemId)
            }
        }

        fun onLetterEntered(
            cell: CrosswordCell,
            letter: String,
        ) {
            updateLoaded { state ->
                if (!state.isEditable) return@updateLoaded state
                val existing = state.cells[cell] ?: return@updateLoaded state
                if (existing.locked) return@updateLoaded state
                val normalized = letter.trim().takeLast(1).uppercase(Locale.ROOT)
                state.copy(cells = state.cells + (cell to existing.copy(letter = normalized, isIncorrect = false)))
            }
        }

        /** Reveals the next unrevealed letter of the selected word, or of any unfinished word if it's complete. */
        fun onTipRequested() {
            updateLoaded { state ->
                if (!state.canUseTip) return@updateLoaded state
                val target = state.selectedWord?.takeIf { it.revealedLetterCount < it.length }
                    ?: state.words.firstOrNull { it.revealedLetterCount < it.length }
                    ?: return@updateLoaded state

                val offset = target.revealedLetterCount
                val cell = target.cellAt(offset)
                val letter = target.expectedText.uppercase(Locale.ROOT)[offset].toString()
                val updatedWord = target.copy(revealedLetterCount = offset + 1)

                state.copy(
                    words = state.words.map { if (it.vocabularyItemId == target.vocabularyItemId) updatedWord else it },
                    cells = state.cells + (cell to CrosswordCellState(letter = letter, locked = true)),
                    selectedWordId = target.vocabularyItemId,
                )
            }
        }

        fun onCheck() {
            val state = _uiState.value as? CrosswordUiState.Loaded ?: return
            if (!state.canCheck) return
            updateLoaded { it.copy(isSubmitting = true, submitFailed = false) }
            viewModelScope.launch(dispatchers.io) {
                val submissions = state.words.map { word ->
                    CrosswordWordSubmission(
                        vocabularyItemId = word.vocabularyItemId,
                        expectedText = word.expectedText,
                        submittedText = state.enteredText(word),
                        tipUsed = word.revealedLetterCount > 0,
                    )
                }
                val response = runCatching {
                    submitCrosswordUseCase(SubmitCrosswordRequest(sessionId, submissions))
                }.getOrElse {
                    // Leave the grid editable and let the user try Check again rather than
                    // stranding them on a screen whose only action is permanently disabled.
                    updateLoaded { it.copy(isSubmitting = false, submitFailed = true) }
                    return@launch
                }

                lastSessionResultsHolder.wordResults = response.wordResults.map { result ->
                    val word = state.words.first { it.vocabularyItemId == result.vocabularyItemId }
                    WordResultEntry(
                        word = result.expectedText,
                        translation = word.clueText,
                        outcome = if (result.outcome == CrosswordWordOutcome.CORRECT) {
                            AnswerState.Correct
                        } else {
                            AnswerState.Incorrect(result.expectedText)
                        },
                        tipUsed = result.tipUsed,
                    )
                }
                pendingResult = response.wordResults.let { results ->
                    val correct = results.count { it.outcome == CrosswordWordOutcome.CORRECT }
                    SessionNavigationEvent.SessionComplete(
                        correct = correct,
                        incorrect = results.size - correct,
                        skipped = 0,
                        tipsUsed = results.count { it.tipUsed },
                    )
                }

                markIncorrectCells(
                    response.wordResults
                        .filter { it.outcome == CrosswordWordOutcome.INCORRECT }
                        .map { it.vocabularyItemId }
                        .toSet(),
                )
            }
        }

        /**
         * Called from the UI's "Next" button. Navigation is deliberately not emitted straight from
         * [onCheck]: doing so pops this screen instantly, so the user would never actually see the
         * incorrect cells or the revealed answers that Check just produced.
         */
        fun onContinue() {
            val state = _uiState.value as? CrosswordUiState.Loaded ?: return
            if (!state.awaitingContinue) return
            val result = pendingResult ?: return
            viewModelScope.launch { _navigationEvents.emit(result) }
        }

        private fun markIncorrectCells(incorrectWordIds: Set<Long>) {
            updateLoaded { state ->
                val incorrectCells = state.words
                    .filter { it.vocabularyItemId in incorrectWordIds }
                    .flatMap { it.occupiedCells() }
                    .toSet()
                state.copy(
                    cells = state.cells.mapValues { (cell, cellState) ->
                        cellState.copy(isIncorrect = cell in incorrectCells)
                    },
                    answerState = if (incorrectWordIds.isEmpty()) AnswerState.Correct else AnswerState.Incorrect(),
                    isSubmitting = false,
                )
            }
        }

        private inline fun updateLoaded(transform: (CrosswordUiState.Loaded) -> CrosswordUiState.Loaded) {
            _uiState.update { current -> if (current is CrosswordUiState.Loaded) transform(current) else current }
        }
    }
