package com.lexicon.interactors.pronunciation

data class StartPronunciationSessionRequest(
    val stepCount: Int? = null,
    // Empty means the whole study set; a lesson passes its own words here.
    val vocabularyIds: List<Long> = emptyList(),
)

interface StartPronunciationSessionUseCase {
    suspend operator fun invoke(request: StartPronunciationSessionRequest): PronunciationSessionResponse
}
