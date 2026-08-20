package com.lexicon.interactors.puzzle

data class StartPuzzleSessionRequest(
    val stepCount: Int? = null,
    val vocabularyIds: List<Long> = emptyList(),
)

interface StartPuzzleSessionUseCase {
    suspend operator fun invoke(request: StartPuzzleSessionRequest): PuzzleSessionResponse
}
