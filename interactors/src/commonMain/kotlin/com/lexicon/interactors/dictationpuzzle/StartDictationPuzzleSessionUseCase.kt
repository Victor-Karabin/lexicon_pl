package com.lexicon.interactors.dictationpuzzle

data class StartDictationPuzzleSessionRequest(
    val stepCount: Int? = null,
    val vocabularyIds: List<Long> = emptyList(),
)

interface StartDictationPuzzleSessionUseCase {
    suspend operator fun invoke(request: StartDictationPuzzleSessionRequest): DictationPuzzleSessionResponse
}
