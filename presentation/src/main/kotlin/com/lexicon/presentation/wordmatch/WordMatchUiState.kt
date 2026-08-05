package com.lexicon.presentation.wordmatch

import com.lexicon.presentation.common.AnswerState

data class WordMatchColumnItem(val vocabularyItemId: Long, val text: String)

data class WordMatchUiState(
    val isLoading: Boolean = true,
    val stepIndex: Int = 0,
    val totalSteps: Int = 0,
    val leftColumn: List<WordMatchColumnItem> = emptyList(),
    val rightColumn: List<WordMatchColumnItem> = emptyList(),
    val matchedIds: Set<Long> = emptySet(),
    val selectedLeftId: Long? = null,
    val selectedRightId: Long? = null,
    val incorrectFlashIds: Set<Long> = emptySet(),
    val incorrectAttempts: Int = 0,
    val answerState: AnswerState = AnswerState.UNANSWERED,
    val isSessionComplete: Boolean = false,
) {
    val isInteractive: Boolean get() = answerState == AnswerState.UNANSWERED
    val canSkip: Boolean get() = isInteractive
    val awaitingNext: Boolean get() = answerState == AnswerState.SKIPPED
}
