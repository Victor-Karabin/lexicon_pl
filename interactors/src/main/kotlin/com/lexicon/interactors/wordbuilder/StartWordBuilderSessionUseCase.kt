package com.lexicon.interactors.wordbuilder

data class StartWordBuilderSessionRequest(
    val stepCount: Int = DEFAULT_STEP_COUNT,
) {
    companion object {
        const val DEFAULT_STEP_COUNT = 10
    }
}

interface StartWordBuilderSessionUseCase {
    suspend operator fun invoke(request: StartWordBuilderSessionRequest): WordBuilderSessionResponse
}
