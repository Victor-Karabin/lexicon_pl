package com.lexicon.pl.interactors.pronunciation

data class StartPronunciationSessionRequest(
    val stepCount: Int = DEFAULT_STEP_COUNT,
) {
    companion object {
        const val DEFAULT_STEP_COUNT = 10
    }
}

interface StartPronunciationSessionUseCase {
    suspend operator fun invoke(request: StartPronunciationSessionRequest): PronunciationSessionResponse
}
