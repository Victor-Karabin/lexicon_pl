package com.lexicon.interactors.dictation

data class StartDictationSessionRequest(
    val stepCount: Int? = null,
)

interface StartDictationSessionUseCase {
    suspend operator fun invoke(request: StartDictationSessionRequest): DictationSessionResponse
}
