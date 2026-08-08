package com.lexicon.interactors.wordmatch

data class StartWordMatchSessionRequest(
    val stepCount: Int? = null,
)

interface StartWordMatchSessionUseCase {
    suspend operator fun invoke(request: StartWordMatchSessionRequest): WordMatchSessionResponse
}
