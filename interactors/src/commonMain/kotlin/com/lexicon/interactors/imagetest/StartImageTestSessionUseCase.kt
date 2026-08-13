package com.lexicon.interactors.imagetest

data class StartImageTestSessionRequest(
    val stepCount: Int? = null,
    val optionCount: Int = DEFAULT_OPTION_COUNT,
    // Empty means the whole study set; a lesson passes its own words here.
    val vocabularyIds: List<Long> = emptyList(),
) {
    companion object {
        const val DEFAULT_OPTION_COUNT = 6
    }
}

interface StartImageTestSessionUseCase {
    suspend operator fun invoke(request: StartImageTestSessionRequest): ImageTestSessionResponse
}
