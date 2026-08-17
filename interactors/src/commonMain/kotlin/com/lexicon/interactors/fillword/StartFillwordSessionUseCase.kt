package com.lexicon.interactors.fillword

sealed interface FillwordSessionResult {
    data class Ready(
        val sessionId: String,
        val puzzle: FillwordPuzzle,
    ) : FillwordSessionResult

    data object NoFavourites : FillwordSessionResult

    data object Offline : FillwordSessionResult

    data class Refused(val reason: String) : FillwordSessionResult
}

interface StartFillwordSessionUseCase {
    suspend operator fun invoke(): FillwordSessionResult
}
