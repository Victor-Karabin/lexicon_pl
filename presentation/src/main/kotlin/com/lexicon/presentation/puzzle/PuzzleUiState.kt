package com.lexicon.presentation.puzzle

import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LetterTile
import com.lexicon.presentation.common.revealedAnswer

sealed interface PuzzleUiState {
    data object Loading : PuzzleUiState

    data object Unavailable : PuzzleUiState

    data class Loaded(
        val stepIndex: Int = 0,
        val totalSteps: Int = 0,
        val imageUrl: String? = null,
        val clueText: String = "",
        val stepTiles: List<LetterTile> = emptyList(),
        val placedTiles: List<LetterTile> = emptyList(),
        val answerState: AnswerState = AnswerState.Unanswered,
        val tipTranslation: String? = null,
        val tipUsed: Boolean = false,
        val isSessionComplete: Boolean = false,
    ) : PuzzleUiState {
        val availableTiles: List<LetterTile> get() = stepTiles.filterNot { tile -> placedTiles.any { it.id == tile.id } }
        val builtAnswer: String get() = placedTiles.joinToString(separator = "") { it.char.toString() }

        val revealedAnswer: String? get() = answerState.revealedAnswer
        val isEditable: Boolean get() = answerState is AnswerState.Unanswered
        val canCheck: Boolean get() = isEditable && stepTiles.isNotEmpty() && availableTiles.isEmpty()
        val canUseTip: Boolean get() = isEditable && !tipUsed
        val canSkip: Boolean get() = isEditable
        val canUndo: Boolean get() = isEditable && placedTiles.isNotEmpty()

        val awaitingNext: Boolean get() = answerState is AnswerState.Incorrect
    }
}
