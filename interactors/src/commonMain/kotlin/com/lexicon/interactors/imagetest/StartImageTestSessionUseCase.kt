package com.lexicon.interactors.imagetest

data class StartImageTestSessionRequest(
    val stepCount: Int? = null,
    val optionCount: Int = DEFAULT_OPTION_COUNT,
    val vocabularyIds: List<Long> = emptyList(),
) {
    companion object {
        const val DEFAULT_OPTION_COUNT = 6
    }
}

interface StartImageTestSessionUseCase {
    suspend operator fun invoke(request: StartImageTestSessionRequest): ImageTestSessionResponse
}
