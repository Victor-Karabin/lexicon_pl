package com.lexicon.interactors.dictationpuzzle

data class StartDictationPuzzleSessionRequest(
    /** Null uses the step count configured in Settings; pass a value only to override it. */
    val stepCount: Int? = null,
)

interface StartDictationPuzzleSessionUseCase {
    suspend operator fun invoke(request: StartDictationPuzzleSessionRequest): DictationPuzzleSessionResponse
}
