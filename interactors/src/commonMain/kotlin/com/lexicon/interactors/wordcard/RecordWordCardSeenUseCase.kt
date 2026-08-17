package com.lexicon.interactors.wordcard

interface RecordWordCardSeenUseCase {
    suspend operator fun invoke(request: RecordWordCardSeenRequest)
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
