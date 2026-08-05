package com.lexicon.pl.interactors.dictation

data class StartDictationSessionRequest(
    val stepCount: Int = DEFAULT_STEP_COUNT,
) {
    companion object {
        const val DEFAULT_STEP_COUNT = 10
    }
}

interface StartDictationSessionUseCase {
    suspend operator fun invoke(request: StartDictationSessionRequest): DictationSessionResponse
}
