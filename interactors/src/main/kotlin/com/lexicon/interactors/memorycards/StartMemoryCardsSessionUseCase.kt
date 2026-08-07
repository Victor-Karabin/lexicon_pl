package com.lexicon.interactors.memorycards

data class StartMemoryCardsSessionRequest(
    /** Null uses the step count configured in Settings; pass a value only to override it. */
    val stepCount: Int? = null,
    val pairsPerStep: Int = DEFAULT_PAIRS_PER_STEP,
) {
    companion object {
        const val DEFAULT_PAIRS_PER_STEP = 6
    }
}

interface StartMemoryCardsSessionUseCase {
    suspend operator fun invoke(request: StartMemoryCardsSessionRequest): MemoryCardsSessionResponse
}
