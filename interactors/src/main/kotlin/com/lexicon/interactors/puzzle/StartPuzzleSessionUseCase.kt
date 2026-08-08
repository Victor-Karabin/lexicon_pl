package com.lexicon.interactors.puzzle

data class StartPuzzleSessionRequest(
    val stepCount: Int? = null,
)

interface StartPuzzleSessionUseCase {
    suspend operator fun invoke(request: StartPuzzleSessionRequest): PuzzleSessionResponse
}
