package com.lexicon.interactors.puzzle

data class StartPuzzleSessionRequest(
    val stepCount: Int? = null,
    // Empty means the whole study set; a lesson passes its own words here.
    val vocabularyIds: List<Long> = emptyList(),
)

interface StartPuzzleSessionUseCase {
    suspend operator fun invoke(request: StartPuzzleSessionRequest): PuzzleSessionResponse
}
