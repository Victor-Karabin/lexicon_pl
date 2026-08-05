package com.lexicon.pl.interactors.wordmatch

enum class WordMatchStepOutcome { CORRECT, INCORRECT, SKIPPED }

data class WordMatchPairResponse(
    val vocabularyItemId: Long,
    val word: String,
    val translation: String,
)

data class WordMatchStepResponse(
    val stepIndex: Int,
    val pairs: List<WordMatchPairResponse>,
)

data class WordMatchSessionResponse(
    val sessionId: String,
    val steps: List<WordMatchStepResponse>,
)
