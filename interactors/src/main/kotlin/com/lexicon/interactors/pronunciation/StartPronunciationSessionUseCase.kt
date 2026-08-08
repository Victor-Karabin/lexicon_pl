package com.lexicon.interactors.pronunciation

data class StartPronunciationSessionRequest(
    val stepCount: Int? = null,
)

interface StartPronunciationSessionUseCase {
    suspend operator fun invoke(request: StartPronunciationSessionRequest): PronunciationSessionResponse
}
