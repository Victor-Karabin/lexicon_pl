package com.lexicon.interactors.puzzle

enum class PuzzleStepOutcome { CORRECT, INCORRECT, SKIPPED }

data class PuzzleStepResponse(
    val stepIndex: Int,
    val vocabularyItemId: Long,
    val expectedText: String,
    val imageUrl: String?,
    val clueText: String,
)

data class PuzzleSessionResponse(
    val sessionId: String,
    val steps: List<PuzzleStepResponse>,
)
