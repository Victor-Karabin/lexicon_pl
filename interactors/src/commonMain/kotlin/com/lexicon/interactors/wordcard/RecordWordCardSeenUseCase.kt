package com.lexicon.interactors.wordcard

interface RecordWordCardSeenUseCase {
    suspend operator fun invoke(request: RecordWordCardSeenRequest)
}

data class RecordWordCardSeenRequest(
    val sessionId: String,
    val stepIndex: Int,
    val vocabularyItemId: Long,
    val text: String,
)
