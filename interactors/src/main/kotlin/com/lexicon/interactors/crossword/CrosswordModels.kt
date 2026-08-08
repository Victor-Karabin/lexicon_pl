package com.lexicon.interactors.crossword

enum class CrosswordDirection { ACROSS, DOWN }

data class CrosswordWordPlacement(
    val vocabularyItemId: Long,
    val expectedText: String,
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
