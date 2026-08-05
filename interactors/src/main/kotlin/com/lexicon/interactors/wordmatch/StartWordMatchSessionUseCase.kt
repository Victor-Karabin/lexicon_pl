package com.lexicon.interactors.wordmatch

data class StartWordMatchSessionRequest(
    val stepCount: Int = DEFAULT_STEP_COUNT,
    val pairsPerStep: Int = DEFAULT_PAIRS_PER_STEP,
) {
    companion object {
        const val DEFAULT_STEP_COUNT = 10
        const val DEFAULT_PAIRS_PER_STEP = 6
    }
}

interface StartWordMatchSessionUseCase {
    suspend operator fun invoke(request: StartWordMatchSessionRequest): WordMatchSessionResponse
}
