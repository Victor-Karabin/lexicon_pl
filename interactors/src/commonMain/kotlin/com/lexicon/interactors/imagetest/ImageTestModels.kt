package com.lexicon.interactors.imagetest

enum class ImageTestStepOutcome { CORRECT, INCORRECT, SKIPPED }

data class ImageTestStepResponse(
    val stepIndex: Int,
    val vocabularyItemId: Long,
    val imageUrl: String?,
    val clueText: String,
    val options: List<String>,
    val correctOption: String,
)

data class ImageTestSessionResponse(
    val sessionId: String,
    val steps: List<ImageTestStepResponse>,
)
