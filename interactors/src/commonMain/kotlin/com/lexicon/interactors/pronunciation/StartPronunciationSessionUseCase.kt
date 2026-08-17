package com.lexicon.interactors.pronunciation

data class StartPronunciationSessionRequest(
    val stepCount: Int? = null,
    val vocabularyIds: List<Long> = emptyList(),
)

interface StartPronunciationSessionUseCase {
    suspend operator fun invoke(request: StartPronunciationSessionRequest): PronunciationSessionResponse
}
