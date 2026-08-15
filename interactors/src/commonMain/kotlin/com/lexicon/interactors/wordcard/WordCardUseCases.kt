package com.lexicon.interactors.wordcard

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

data class WordCardSessionResponse(
    val sessionId: String,
    val steps: List<WordCardStep>,
)

data class StartWordCardSessionRequest(
    val vocabularyIds: List<Long> = emptyList(),
)

interface StartWordCardSessionUseCase {
    suspend operator fun invoke(request: StartWordCardSessionRequest): WordCardSessionResponse
}

/**
 * Records that a card was shown.
 *
 * Recorded rather than ignored so the day knows the session happened and the time
 * counts as studying — but as having been seen, which leaves the word's review
 * schedule and the accuracy figure alone.
 */
data class RecordWordCardSeenRequest(
    val sessionId: String,
    val stepIndex: Int,
    val vocabularyItemId: Long,
    val text: String,
)

interface RecordWordCardSeenUseCase {
    suspend operator fun invoke(request: RecordWordCardSeenRequest)
}
