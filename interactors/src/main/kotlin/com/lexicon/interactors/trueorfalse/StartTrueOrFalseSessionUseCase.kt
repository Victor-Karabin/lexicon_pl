package com.lexicon.interactors.trueorfalse

data class StartTrueOrFalseSessionRequest(
    val stepCount: Int = DEFAULT_STEP_COUNT,
    /** Probability that a step displays the correct translation rather than a distractor. */
    val correctProbability: Double = DEFAULT_CORRECT_PROBABILITY,
) {
    companion object {
        const val DEFAULT_STEP_COUNT = 10
        const val DEFAULT_CORRECT_PROBABILITY = 0.5
    }
}

interface StartTrueOrFalseSessionUseCase {
    suspend operator fun invoke(request: StartTrueOrFalseSessionRequest): TrueOrFalseSessionResponse
}
