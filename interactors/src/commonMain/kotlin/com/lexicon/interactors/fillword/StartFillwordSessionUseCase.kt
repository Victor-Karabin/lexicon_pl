package com.lexicon.interactors.fillword

sealed interface FillwordSessionResult {
    data class Ready(
        val sessionId: String,
        val puzzle: FillwordPuzzle,
    ) : FillwordSessionResult

    data object NoFavourites : FillwordSessionResult
}

interface StartFillwordSessionUseCase {
    suspend operator fun invoke(): FillwordSessionResult
}
