package com.lexicon.interactors.crossword

enum class CrosswordDirection { ACROSS, DOWN }

data class CrosswordWordPlacement(
    val vocabularyItemId: Long,
    /** Target-language text to spell out; never shown directly. Always a single word — phrases are excluded. */
    val expectedText: String,
    val imageUrl: String?,
    /** Shown instead of the image when [imageUrl] is null or fails to load. */
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
