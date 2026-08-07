package com.lexicon.interactors.crossword

enum class CrosswordDirection { ACROSS, DOWN }

data class CrosswordWordPlacement(
    val vocabularyItemId: Long,
    /** Target-language text to spell out; never shown directly. Always a single word — phrases are excluded. */
    val expectedText: String,
    /** Base-language word shown as the clue. */
    val clueText: String,
    val row: Int,
    val col: Int,
    val direction: CrosswordDirection,
)

data class CrosswordSessionResponse(
    val sessionId: String,
    val words: List<CrosswordWordPlacement>,
    val rowCount: Int,
    val colCount: Int,
)
