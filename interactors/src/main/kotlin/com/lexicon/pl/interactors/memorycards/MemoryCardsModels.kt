package com.lexicon.pl.interactors.memorycards

enum class MemoryCardsStepOutcome { CORRECT, INCORRECT, SKIPPED }

data class MemoryCardsPairResponse(
    val vocabularyItemId: Long,
    val imageUrl: String?,
    /** Shown on the image card instead of the image when it's missing or fails to load. */
    val imageFallbackText: String,
    /** Shown on the text card. */
    val text: String,
)

data class MemoryCardsStepResponse(
    val stepIndex: Int,
    val pairs: List<MemoryCardsPairResponse>,
)

data class MemoryCardsSessionResponse(
    val sessionId: String,
    val steps: List<MemoryCardsStepResponse>,
)
