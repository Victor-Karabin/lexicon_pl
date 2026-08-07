package com.lexicon.interactors.dictation

data class StartDictationSessionRequest(
    /** Null uses the step count configured in Settings; pass a value only to override it. */
    val stepCount: Int? = null,
)

interface StartDictationSessionUseCase {
    suspend operator fun invoke(request: StartDictationSessionRequest): DictationSessionResponse
}
