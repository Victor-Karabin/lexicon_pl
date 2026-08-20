package com.lexicon.interactors.wordmatch

data class StartWordMatchSessionRequest(
    val stepCount: Int? = null,
    val vocabularyIds: List<Long> = emptyList(),
)

interface StartWordMatchSessionUseCase {
    suspend operator fun invoke(request: StartWordMatchSessionRequest): WordMatchSessionResponse
}
