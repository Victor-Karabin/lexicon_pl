package com.lexicon.interactors.presets

enum class WordDraftProblem {
    MISSING_TEXT,
    MISSING_TRANSLATION,

    ALREADY_EXISTS,
}

class WordDraftException(val problem: WordDraftProblem) : Exception(problem.name)
