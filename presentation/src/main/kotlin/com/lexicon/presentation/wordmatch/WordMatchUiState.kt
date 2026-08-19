package com.lexicon.presentation.wordmatch

data class WordMatchColumnItem(val vocabularyItemId: Long, val text: String)

sealed interface WordMatchUiState {
    data object Loading : WordMatchUiState

    data object Unavailable : WordMatchUiState

    data class Loaded(
        val stepIndex: Int = 0,
        val totalSteps: Int = 0,
        val leftColumn: List<WordMatchColumnItem> = emptyList(),
        val rightColumn: List<WordMatchColumnItem> = emptyList(),
        val matchedIds: Set<Long> = emptySet(),
        val selectedLeftId: Long? = null,
        val selectedRightId: Long? = null,
        val incorrectLeftId: Long? = null,
        val incorrectRightId: Long? = null,
        val incorrectAttempts: Int = 0,
        val isSessionComplete: Boolean = false,
    ) : WordMatchUiState
}
