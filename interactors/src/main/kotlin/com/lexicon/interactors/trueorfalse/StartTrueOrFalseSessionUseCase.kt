package com.lexicon.interactors.trueorfalse

/**
 * This training is timed (see [com.lexicon.presentation.trueorfalse.TrueOrFalseViewModel]'s
 * 60-second timer), not step-count-limited — the user answers as many items as they can before
 * time runs out. [poolSize] is a generous ceiling on how many are fetched, not a target to reach.
 */
data class StartTrueOrFalseSessionRequest(
    val poolSize: Int = DEFAULT_POOL_SIZE,
    /** Probability that a step displays the correct translation rather than a distractor. */
    val correctProbability: Double = DEFAULT_CORRECT_PROBABILITY,
) {
    companion object {
        const val DEFAULT_POOL_SIZE = 50
        const val DEFAULT_CORRECT_PROBABILITY = 0.5
    }
}

interface StartTrueOrFalseSessionUseCase {
    suspend operator fun invoke(request: StartTrueOrFalseSessionRequest): TrueOrFalseSessionResponse
}
