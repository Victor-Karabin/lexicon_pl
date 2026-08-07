package com.lexicon.interactors.puzzle

data class StartPuzzleSessionRequest(
    val stepCount: Int = DEFAULT_STEP_COUNT,
) {
    companion object {
        const val DEFAULT_STEP_COUNT = 10
    }
}

interface StartPuzzleSessionUseCase {
    suspend operator fun invoke(request: StartPuzzleSessionRequest): PuzzleSessionResponse
}
