package com.lexicon.pl.interactors.puzzle

enum class PuzzleStepOutcome { CORRECT, INCORRECT, SKIPPED }

data class PuzzleStepResponse(
    val stepIndex: Int,
    val vocabularyItemId: Long,
    /** Target spelling to reconstruct from letter tiles; never shown directly. */
    val expectedText: String,
    val imageUrl: String?,
    /** Shown instead of the image when [imageUrl] is null or fails to load. */
    val clueText: String,
)

data class PuzzleSessionResponse(
    val sessionId: String,
    val steps: List<PuzzleStepResponse>,
)
