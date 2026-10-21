package com.lexicon.interactors.fillword

data class SubmitFillwordRequest(
    val sessionId: String,
    val foundWords: Set<String>,
)

interface SubmitFillwordUseCase {
    suspend operator fun invoke(request: SubmitFillwordRequest)
}
