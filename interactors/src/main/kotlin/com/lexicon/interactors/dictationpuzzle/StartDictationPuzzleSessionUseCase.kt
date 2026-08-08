package com.lexicon.interactors.dictationpuzzle

data class StartDictationPuzzleSessionRequest(
    val stepCount: Int? = null,
)

interface StartDictationPuzzleSessionUseCase {
    suspend operator fun invoke(request: StartDictationPuzzleSessionRequest): DictationPuzzleSessionResponse
}
