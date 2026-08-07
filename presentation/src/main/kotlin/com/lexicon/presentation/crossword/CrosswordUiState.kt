package com.lexicon.presentation.crossword

import com.lexicon.interactors.crossword.CrosswordDirection
import com.lexicon.presentation.common.AnswerState

data class CrosswordCell(val row: Int, val col: Int)

data class CrosswordCellState(
    val letter: String = "",
    /** Revealed via Tip; locked cells can't be edited further. */
    val locked: Boolean = false,
    /** Set after Check for every cell of a word that didn't match. */
    val isIncorrect: Boolean = false,
)

data class CrosswordWordUi(
    val vocabularyItemId: Long,
    val row: Int,
    val col: Int,
    val direction: CrosswordDirection,
    val length: Int,
    val imageUrl: String?,
    val clueText: String,
    /** Target spelling to reconstruct in the grid; never shown directly. */
    val expectedText: String,
    /** How many letters, from the start, have been revealed (and locked) via Tip. */
    val revealedLetterCount: Int = 0,
)

fun CrosswordWordUi.cellAt(offset: Int): CrosswordCell =
    if (direction == CrosswordDirection.ACROSS) CrosswordCell(row, col + offset) else CrosswordCell(row + offset, col)

fun CrosswordWordUi.occupiedCells(): List<CrosswordCell> = (0 until length).map { cellAt(it) }

sealed interface CrosswordUiState {
    data object Loading : CrosswordUiState

    data class Loaded(
        val words: List<CrosswordWordUi> = emptyList(),
        val cells: Map<CrosswordCell, CrosswordCellState> = emptyMap(),
        val rowCount: Int = 0,
        val colCount: Int = 0,
        val selectedWordId: Long? = null,
        val answerState: AnswerState = AnswerState.Unanswered,
        val isSessionComplete: Boolean = false,
        /** True while Check is mid-flight — disables the action row so a rapid double-tap can't fire twice. */
        val isSubmitting: Boolean = false,
    ) : CrosswordUiState {
        val isEditable: Boolean get() = answerState is AnswerState.Unanswered && !isSessionComplete && !isSubmitting
        val canCheck: Boolean get() = isEditable
        val canUseTip: Boolean get() = isEditable && words.any { it.revealedLetterCount < it.length }
        val selectedWord: CrosswordWordUi? get() = words.find { it.vocabularyItemId == selectedWordId }

        fun enteredText(word: CrosswordWordUi): String = word.occupiedCells().joinToString("") { cell -> cells[cell]?.letter.orEmpty() }
    }
}
