package com.lexicon.interactors.wordmatch

data class StartWordMatchSessionRequest(
    val stepCount: Int? = null,
    // Empty means the whole study set; a lesson passes its own words here.
    val vocabularyIds: List<Long> = emptyList(),
)

interface StartWordMatchSessionUseCase {
    suspend operator fun invoke(request: StartWordMatchSessionRequest): WordMatchSessionResponse
}
