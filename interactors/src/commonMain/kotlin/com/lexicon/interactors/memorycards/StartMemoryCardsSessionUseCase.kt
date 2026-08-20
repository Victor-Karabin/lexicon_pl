package com.lexicon.interactors.memorycards

data class StartMemoryCardsSessionRequest(
    val stepCount: Int? = null,
    val pairsPerStep: Int = DEFAULT_PAIRS_PER_STEP,
    val vocabularyIds: List<Long> = emptyList(),
) {
    companion object {
        const val DEFAULT_PAIRS_PER_STEP = 6
    }
}

interface StartMemoryCardsSessionUseCase {
    suspend operator fun invoke(request: StartMemoryCardsSessionRequest): MemoryCardsSessionResponse
}
