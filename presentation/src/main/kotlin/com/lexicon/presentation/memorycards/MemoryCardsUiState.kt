package com.lexicon.presentation.memorycards

import com.lexicon.presentation.common.AnswerState

data class MemoryCard(
    val cardId: Int,
    val vocabularyItemId: Long,
    val isImageCard: Boolean,
    val imageUrl: String?,
    val text: String,
)

data class MemoryCardsUiState(
    val isLoading: Boolean = true,
    val stepIndex: Int = 0,
    val totalSteps: Int = 0,
    val cards: List<MemoryCard> = emptyList(),
    val flippedCardIds: List<Int> = emptyList(),
    val matchedItemIds: Set<Long> = emptySet(),
    val incorrectFlashCardIds: Set<Int> = emptySet(),
    val incorrectAttempts: Int = 0,
    val answerState: AnswerState = AnswerState.UNANSWERED,
    val isSessionComplete: Boolean = false,
) {
    val isInteractive: Boolean get() = answerState == AnswerState.UNANSWERED
    val canSkip: Boolean get() = isInteractive
    val awaitingNext: Boolean get() = answerState == AnswerState.SKIPPED

    fun isFaceUp(card: MemoryCard): Boolean =
        matchedItemIds.contains(card.vocabularyItemId) ||
            flippedCardIds.contains(card.cardId) ||
            incorrectFlashCardIds.contains(card.cardId)
}
