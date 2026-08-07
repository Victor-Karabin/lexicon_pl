package com.lexicon.interactors.pronunciation

data class StartPronunciationSessionRequest(
    /** Null uses the step count configured in Settings; pass a value only to override it. */
    val stepCount: Int? = null,
)

interface StartPronunciationSessionUseCase {
    suspend operator fun invoke(request: StartPronunciationSessionRequest): PronunciationSessionResponse
}
