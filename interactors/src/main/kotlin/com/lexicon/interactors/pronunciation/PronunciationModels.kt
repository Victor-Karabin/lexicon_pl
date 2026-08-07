package com.lexicon.interactors.pronunciation

enum class PronunciationStepOutcome { CORRECT, INCORRECT, SKIPPED }

data class PronunciationStepResponse(
    val stepIndex: Int,
    val vocabularyItemId: Long,
    val expectedText: String,
    val clueText: String,
    /** IPA phonetic transcription, revealed via Tip to help with pronunciation. */
    val transcription: String,
)

data class PronunciationSessionResponse(
    val sessionId: String,
    val steps: List<PronunciationStepResponse>,
)
