package com.lexicon.presentation.dictationpuzzle

import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LetterTile

data class DictationPuzzleUiState(
    val isLoading: Boolean = true,
    val stepIndex: Int = 0,
    val totalSteps: Int = 0,
    /** The full shuffled tile set for the current step; stable identity used to derive available/placed. */
    val stepTiles: List<LetterTile> = emptyList(),
    val placedTiles: List<LetterTile> = emptyList(),
    val answerState: AnswerState = AnswerState.UNANSWERED,
    val revealedAnswer: String? = null,
    val tipUsed: Boolean = false,
    val isSessionComplete: Boolean = false,
) {
    val availableTiles: List<LetterTile> get() = stepTiles.filterNot { tile -> placedTiles.any { it.id == tile.id } }
    val builtAnswer: String get() = placedTiles.joinToString(separator = "") { it.char.toString() }
    val isEditable: Boolean get() = answerState == AnswerState.UNANSWERED
    val canCheck: Boolean get() = isEditable && stepTiles.isNotEmpty() && availableTiles.isEmpty()
    val canUseTip: Boolean get() = isEditable && !tipUsed
    val canSkip: Boolean get() = isEditable
    val awaitingNext: Boolean get() = answerState == AnswerState.INCORRECT || answerState == AnswerState.SKIPPED
}
