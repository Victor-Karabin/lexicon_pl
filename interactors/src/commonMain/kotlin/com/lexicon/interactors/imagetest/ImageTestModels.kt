package com.lexicon.interactors.imagetest

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
