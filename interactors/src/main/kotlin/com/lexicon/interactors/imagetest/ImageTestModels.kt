package com.lexicon.interactors.imagetest

enum class ImageTestStepOutcome { CORRECT, INCORRECT, SKIPPED }

data class ImageTestStepResponse(
    val stepIndex: Int,
    val vocabularyItemId: Long,
    val imageUrl: String?,
    /** Shown instead of the image when it's missing or fails to load. */
    val clueText: String,
    /** Shuffled, includes exactly one correct translation among the distractors. */
    val options: List<String>,
    val correctOption: String,
)

data class ImageTestSessionResponse(
    val sessionId: String,
    val steps: List<ImageTestStepResponse>,
)
