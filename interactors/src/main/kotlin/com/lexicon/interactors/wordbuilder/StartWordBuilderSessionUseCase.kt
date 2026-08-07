package com.lexicon.interactors.wordbuilder

data class StartWordBuilderSessionRequest(
    /** Null uses the step count configured in Settings; pass a value only to override it. */
    val stepCount: Int? = null,
)

interface StartWordBuilderSessionUseCase {
    suspend operator fun invoke(request: StartWordBuilderSessionRequest): WordBuilderSessionResponse
}
