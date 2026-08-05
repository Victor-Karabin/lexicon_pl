package com.lexicon.interactors.dictationpuzzle

data class StartDictationPuzzleSessionRequest(
    val stepCount: Int = DEFAULT_STEP_COUNT,
) {
    companion object {
        const val DEFAULT_STEP_COUNT = 10
    }
}

interface StartDictationPuzzleSessionUseCase {
    suspend operator fun invoke(request: StartDictationPuzzleSessionRequest): DictationPuzzleSessionResponse
}
