package com.lexicon.interactors.dictation

data class StartDictationSessionRequest(
    val stepCount: Int? = null,
    val vocabularyIds: List<Long> = emptyList(),
)

interface StartDictationSessionUseCase {
    suspend operator fun invoke(request: StartDictationSessionRequest): DictationSessionResponse
}
