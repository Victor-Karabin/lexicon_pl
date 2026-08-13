package com.lexicon.interactors.dictationpuzzle

enum class DictationPuzzleStepOutcome { CORRECT, INCORRECT, SKIPPED }

data class DictationPuzzleStepResponse(
    val stepIndex: Int,
    val vocabularyItemId: Long,
    val expectedText: String,
    val translationText: String,
)

data class DictationPuzzleSessionResponse(
    val sessionId: String,
    val steps: List<DictationPuzzleStepResponse>,
)
