package com.lexicon.presentation.crossword

import androidx.lifecycle.SavedStateHandle
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
import com.lexicon.presentation.common.trainingVocabularyIds
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
        savedStateHandle: SavedStateHandle,
        private val startSessionUseCase: StartCrosswordSessionUseCase,
        private val submitCrosswordUseCase: SubmitCrosswordUseCase,
        private val dispatchers: DispatcherProvider,
        private val lastSessionResultsHolder: LastSessionResultsHolder,
    ) : ViewModel() {
        private val vocabularyIds = savedStateHandle.trainingVocabularyIds()

        private val _uiState = MutableStateFlow<CrosswordUiState>(CrosswordUiState.Loading)
        val uiState: StateFlow<CrosswordUiState> = _uiState.asStateFlow()

        private val _navigationEvents = MutableSharedFlow<SessionNavigationEvent>()
        val navigationEvents: SharedFlow<SessionNavigationEvent> = _navigationEvents.asSharedFlow()

        private lateinit var sessionId: String

        init {
            startSession()
        }

        private fun startSession() {
            viewModelScope.launch(dispatchers.io) {
                val response = startSessionUseCase(StartCrosswordSessionRequest(vocabularyIds = vocabularyIds))
                sessionId = response.sessionId
                val words = response.words.map { placement ->
                    CrosswordWordUi(
                        vocabularyItemId = placement.vocabularyItemId,
                        row = placement.row,
                        col = placement.col,
                        direction = placement.direction,
                        length = placement.expectedText.length,
                        clueText = placement.clueText,
                        expectedText = placement.expectedText,
                    )
                }
                val firstWord = words.firstOrNull()
                _uiState.update {
                    CrosswordUiState.Loaded(
                        words = words,
                        cells = words.flatMap { it.occupiedCells() }.associateWith { CrosswordCellState() },
                        rowCount = response.rowCount,
                        colCount = response.colCount,
                        selectedWordId = firstWord?.vocabularyItemId,
                        focusedCell = firstWord?.cellAt(0),
                    )
                }
            }
        }

        fun onClueSelected(vocabularyItemId: Long) {
            updateLoaded { state ->
                if (!state.isEditable) return@updateLoaded state
                val word = state.words.find { it.vocabularyItemId == vocabularyItemId } ?: return@updateLoaded state
                state.copy(
                    selectedWordId = vocabularyItemId,
                    focusedCell = state.firstEditableCell(word) ?: word.cellAt(0),
                )
            }
        }

        fun onCellTapped(cell: CrosswordCell) {
            updateLoaded { state ->
                if (!state.isEditable) return@updateLoaded state
                val cellState = state.cells[cell] ?: return@updateLoaded state
                val current = state.selectedWord
                val owner = current?.takeIf { it.occupiedCells().contains(cell) }
                    ?: state.words.firstOrNull { it.occupiedCells().contains(cell) }
                    ?: return@updateLoaded state

                val cleared = if (cellState.locked) {
                    state.cells
                } else {
                    state.cells + (cell to cellState.copy(letter = ""))
                }
                state.copy(
                    selectedWordId = owner.vocabularyItemId,
                    focusedCell = cell,
                    cells = cleared,
                    submitFailed = false,
                )
            }
        }

        fun onLetterEntered(
            cell: CrosswordCell,
            letter: String,
        ) {
            var completed = false
            updateLoaded { state ->
                if (!state.isEditable) return@updateLoaded state
                val existing = state.cells[cell] ?: return@updateLoaded state
                if (existing.locked) return@updateLoaded state

                val normalized = letter.trim().takeLast(1).uppercase(Locale.ROOT)
                val updated = state.copy(
                    cells = state.cells + (cell to existing.copy(letter = normalized)),
                    submitFailed = false,
                )
                completed = updated.isComplete
                if (normalized.isEmpty()) updated else updated.copy(focusedCell = updated.cellAfter(cell))
            }
            if (completed) submitIfComplete()
        }

        fun onTipRequested() {
            var completed = false
            updateLoaded { state ->
                if (!state.canUseTip) return@updateLoaded state
                val target = state.selectedWord?.takeIf { it.revealedLetterCount < it.length }
                    ?: state.words.firstOrNull { it.revealedLetterCount < it.length }
                    ?: return@updateLoaded state

                val offset = target.revealedLetterCount
                val cell = target.cellAt(offset)
                val letter = target.expectedText.uppercase(Locale.ROOT)[offset].toString()
                val updatedWord = target.copy(revealedLetterCount = offset + 1)

                val updated = state.copy(
                    words = state.words.map { if (it.vocabularyItemId == target.vocabularyItemId) updatedWord else it },
                    cells = state.cells + (cell to CrosswordCellState(letter = letter, locked = true)),
                    selectedWordId = target.vocabularyItemId,
                )
                completed = updated.isComplete
                updated.copy(focusedCell = updated.firstEditableCell(updatedWord) ?: updated.cellAfter(cell))
            }
            if (completed) submitIfComplete()
        }

        private fun submitIfComplete() {
            val state = _uiState.value as? CrosswordUiState.Loaded ?: return
            if (!state.isComplete || !state.isEditable) return
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

                val correct = response.wordResults.count { it.outcome == CrosswordWordOutcome.CORRECT }
                updateLoaded {
                    it.copy(
                        answerState = if (correct == response.wordResults.size) AnswerState.Correct else AnswerState.Incorrect(),
                        isSubmitting = false,
                        focusedCell = null,
                    )
                }
                _navigationEvents.emit(
                    SessionNavigationEvent.SessionComplete(
                        correct = correct,
                        incorrect = response.wordResults.size - correct,
                        skipped = 0,
                        tipsUsed = response.wordResults.count { it.tipUsed },
                    ),
                )
            }
        }

        private inline fun updateLoaded(transform: (CrosswordUiState.Loaded) -> CrosswordUiState.Loaded) {
            _uiState.update { current -> if (current is CrosswordUiState.Loaded) transform(current) else current }
        }
    }

private fun CrosswordUiState.Loaded.cellAfter(cell: CrosswordCell): CrosswordCell? {
    val word = selectedWord ?: return null
    val wordCells = word.occupiedCells()
    val index = wordCells.indexOf(cell)
    if (index == -1) return null
    return wordCells.drop(index + 1).firstOrNull { cells[it]?.locked == false }
}

private fun CrosswordUiState.Loaded.firstEditableCell(word: CrosswordWordUi): CrosswordCell? =
    word.occupiedCells().firstOrNull { cell -> cells[cell]?.let { !it.locked && !it.isFilled } == true }
        ?: word.occupiedCells().firstOrNull { cell -> cells[cell]?.locked == false }
