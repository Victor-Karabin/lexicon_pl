package com.lexicon.presentation.crossword

import com.lexicon.interactors.crossword.CrosswordDirection
import com.lexicon.presentation.common.AnswerState

data class CrosswordCell(val row: Int, val col: Int)

data class CrosswordCellState(
    val letter: String = "",
    val locked: Boolean = false,
) {
    val isFilled: Boolean get() = letter.isNotEmpty()
}

data class CrosswordWordUi(
    val vocabularyItemId: Long,
    val row: Int,
    val col: Int,
    val direction: CrosswordDirection,
    val length: Int,
    val clueText: String,
    val expectedText: String,
    val revealedLetterCount: Int = 0,
)

fun CrosswordWordUi.cellAt(offset: Int): CrosswordCell =
    if (direction == CrosswordDirection.ACROSS) CrosswordCell(row, col + offset) else CrosswordCell(row + offset, col)

fun CrosswordWordUi.occupiedCells(): List<CrosswordCell> = (0 until length).map { cellAt(it) }

sealed interface CrosswordUiState {
    data object Loading : CrosswordUiState

    data object Unavailable : CrosswordUiState

    data class Loaded(
        val words: List<CrosswordWordUi> = emptyList(),
        val cells: Map<CrosswordCell, CrosswordCellState> = emptyMap(),
        val rowCount: Int = 0,
        val colCount: Int = 0,
        val selectedWordId: Long? = null,
        val focusedCell: CrosswordCell? = null,
        val answerState: AnswerState = AnswerState.Unanswered,
        val isSubmitting: Boolean = false,
        val submitFailed: Boolean = false,
    ) : CrosswordUiState {
        val isChecked: Boolean get() = answerState !is AnswerState.Unanswered
        val isEditable: Boolean get() = !isChecked && !isSubmitting
        val canUseTip: Boolean get() = isEditable && words.any { it.revealedLetterCount < it.length }

        val isComplete: Boolean get() = cells.isNotEmpty() && cells.values.all { it.isFilled }

        val selectedWord: CrosswordWordUi? get() = words.find { it.vocabularyItemId == selectedWordId }

        fun enteredText(word: CrosswordWordUi): String = word.occupiedCells().joinToString("") { cell -> cells[cell]?.letter.orEmpty() }
    }
}
