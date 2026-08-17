package com.lexicon.presentation.memorycards

import com.lexicon.presentation.common.AnswerState

data class MemoryCard(
    val cardId: Int,
    val vocabularyItemId: Long,
    val isImageCard: Boolean,
    val imageUrl: String?,
    val text: String,
)

sealed interface MemoryCardsUiState {
    data object Loading : MemoryCardsUiState

    data class Loaded(
        val stepIndex: Int = 0,
        val totalSteps: Int = 0,
        val cards: List<MemoryCard> = emptyList(),
        val flippedCardIds: List<Int> = emptyList(),
        val matchedItemIds: Set<Long> = emptySet(),
        val incorrectFlashCardIds: Set<Int> = emptySet(),
        val incorrectAttempts: Int = 0,
        val answerState: AnswerState = AnswerState.Unanswered,
        val isSessionComplete: Boolean = false,
    ) : MemoryCardsUiState {
        val isInteractive: Boolean get() = answerState is AnswerState.Unanswered
        val canSkip: Boolean get() = isInteractive

        val awaitingNext: Boolean get() = false

        fun isFaceUp(card: MemoryCard): Boolean =
            matchedItemIds.contains(card.vocabularyItemId) ||
                flippedCardIds.contains(card.cardId) ||
                incorrectFlashCardIds.contains(card.cardId)
    }
}
