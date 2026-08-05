package com.lexicon.interactors.imagetest

data class StartImageTestSessionRequest(
    val stepCount: Int = DEFAULT_STEP_COUNT,
    val optionCount: Int = DEFAULT_OPTION_COUNT,
) {
    companion object {
        const val DEFAULT_STEP_COUNT = 10
        const val DEFAULT_OPTION_COUNT = 6
    }
}

interface StartImageTestSessionUseCase {
    suspend operator fun invoke(request: StartImageTestSessionRequest): ImageTestSessionResponse
}
