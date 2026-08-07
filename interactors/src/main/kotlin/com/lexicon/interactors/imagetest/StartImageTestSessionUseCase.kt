package com.lexicon.interactors.imagetest

data class StartImageTestSessionRequest(
    /** Null uses the step count configured in Settings; pass a value only to override it. */
    val stepCount: Int? = null,
    val optionCount: Int = DEFAULT_OPTION_COUNT,
) {
    companion object {
        const val DEFAULT_OPTION_COUNT = 6
    }
}

interface StartImageTestSessionUseCase {
    suspend operator fun invoke(request: StartImageTestSessionRequest): ImageTestSessionResponse
}
