package com.lexicon.interactors.memorycards

data class StartMemoryCardsSessionRequest(
    val stepCount: Int = DEFAULT_STEP_COUNT,
    val pairsPerStep: Int = DEFAULT_PAIRS_PER_STEP,
) {
    companion object {
        const val DEFAULT_STEP_COUNT = 10
        const val DEFAULT_PAIRS_PER_STEP = 6
    }
}

interface StartMemoryCardsSessionUseCase {
    suspend operator fun invoke(request: StartMemoryCardsSessionRequest): MemoryCardsSessionResponse
}
