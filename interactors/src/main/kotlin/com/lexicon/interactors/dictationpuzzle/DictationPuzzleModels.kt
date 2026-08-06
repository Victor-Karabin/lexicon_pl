package com.lexicon.interactors.dictationpuzzle

enum class DictationPuzzleStepOutcome { CORRECT, INCORRECT, SKIPPED }

data class DictationPuzzleStepResponse(
    val stepIndex: Int,
    val vocabularyItemId: Long,
    /** Text to synthesize via TTS and to validate the tile-built answer against. */
    val expectedText: String,
    /** Base-language meaning, shown as the Tip hint — never the learning-language answer itself. */
    val translationText: String,
)

data class DictationPuzzleSessionResponse(
    val sessionId: String,
    val steps: List<DictationPuzzleStepResponse>,
)
