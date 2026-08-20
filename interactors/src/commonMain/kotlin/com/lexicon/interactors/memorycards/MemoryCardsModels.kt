package com.lexicon.interactors.memorycards

data class MemoryCardsPairResponse(
    val vocabularyItemId: Long,
    val imageUrl: String?,
    val imageFallbackText: String,
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
