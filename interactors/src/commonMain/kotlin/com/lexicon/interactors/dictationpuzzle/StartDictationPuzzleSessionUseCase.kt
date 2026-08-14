package com.lexicon.interactors.dictationpuzzle

data class StartDictationPuzzleSessionRequest(
    val stepCount: Int? = null,
    // Empty means the whole study set; a lesson passes its own words here.
    val vocabularyIds: List<Long> = emptyList(),
)

interface StartDictationPuzzleSessionUseCase {
    suspend operator fun invoke(request: StartDictationPuzzleSessionRequest): DictationPuzzleSessionResponse
}
