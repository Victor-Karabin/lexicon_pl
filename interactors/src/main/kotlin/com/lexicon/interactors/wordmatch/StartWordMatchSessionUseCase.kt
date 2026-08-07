package com.lexicon.interactors.wordmatch

/** Word Match is a single step; [stepCount] is the number of pairs in it, matching every other
 *  training's step-count parameter/default rather than introducing a Word-Match-specific knob. */
data class StartWordMatchSessionRequest(
    val stepCount: Int = DEFAULT_STEP_COUNT,
) {
    companion object {
        const val DEFAULT_STEP_COUNT = 10
    }
}

interface StartWordMatchSessionUseCase {
    suspend operator fun invoke(request: StartWordMatchSessionRequest): WordMatchSessionResponse
}
