package com.lexicon.interactors.trueorfalse

data class StartTrueOrFalseSessionRequest(
    val poolSize: Int = DEFAULT_POOL_SIZE,
    val correctProbability: Double = DEFAULT_CORRECT_PROBABILITY,
    // Empty means the whole study set; a lesson passes its own words here.
    val vocabularyIds: List<Long> = emptyList(),
) {
    companion object {
        const val DEFAULT_POOL_SIZE = 50
        const val DEFAULT_CORRECT_PROBABILITY = 0.5
    }
}

interface StartTrueOrFalseSessionUseCase {
    suspend operator fun invoke(request: StartTrueOrFalseSessionRequest): TrueOrFalseSessionResponse
}
