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

/**
 * A word as the deck shows it: everything known about it, and nothing to answer.
 *
 * The one training that asks nothing. Meeting a word and being tested on it are
 * different things, and a training that only shows is what makes the first one
 * possible before the rest of them are fair.
 */
data class WordCardStep(
    val stepIndex: Int,
    val vocabularyItemId: Long,
    val text: String,
    val translation: String,
    val transcription: String,
    val imageUrl: String?,
)
