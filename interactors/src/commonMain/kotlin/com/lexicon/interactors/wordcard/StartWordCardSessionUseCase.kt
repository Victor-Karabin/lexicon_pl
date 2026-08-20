package com.lexicon.interactors.wordcard

interface StartWordCardSessionUseCase {
    suspend operator fun invoke(request: StartWordCardSessionRequest): WordCardSessionResponse
}

data class StartWordCardSessionRequest(
    val vocabularyIds: List<Long> = emptyList(),
)

data class WordCardSessionResponse(
    val sessionId: String,
    val steps: List<WordCardStep>,
)

data class WordCardStep(
    val stepIndex: Int,
    val vocabularyItemId: Long,
    val text: String,
    val translation: String,
    val transcription: String,
    val imageUrl: String?,
)
