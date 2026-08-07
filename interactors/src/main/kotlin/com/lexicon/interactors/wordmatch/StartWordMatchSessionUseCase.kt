package com.lexicon.interactors.wordmatch

/** Word Match is a single step; [stepCount] is the number of pairs in it, matching every other
 *  training's step-count parameter/default rather than introducing a Word-Match-specific knob. */
data class StartWordMatchSessionRequest(
    /** Null uses the step count configured in Settings; pass a value only to override it. */
    val stepCount: Int? = null,
)

interface StartWordMatchSessionUseCase {
    suspend operator fun invoke(request: StartWordMatchSessionRequest): WordMatchSessionResponse
}
