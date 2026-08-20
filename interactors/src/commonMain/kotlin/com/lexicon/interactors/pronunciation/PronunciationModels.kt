package com.lexicon.interactors.pronunciation

data class PronunciationStepResponse(
    val stepIndex: Int,
    val vocabularyItemId: Long,
    val expectedText: String,
    val clueText: String,
    val transcription: String,
)

data class PronunciationSessionResponse(
    val sessionId: String,
    val steps: List<PronunciationStepResponse>,
)
