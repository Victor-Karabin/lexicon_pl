package com.lexicon.interactors.puzzle

data class StartPuzzleSessionRequest(
    /** Null uses the step count configured in Settings; pass a value only to override it. */
    val stepCount: Int? = null,
)

interface StartPuzzleSessionUseCase {
    suspend operator fun invoke(request: StartPuzzleSessionRequest): PuzzleSessionResponse
}
