package com.lexicon.interactors.presets

/** What is wrong with a draft, in the order the form should complain about it. */
enum class WordDraftProblem {
    MISSING_TEXT,
    MISSING_TRANSLATION,

    /** The corpus, or an earlier addition, already has this Polish word. */
    ALREADY_EXISTS,
}

/** Raised by [CreateWordUseCase] so the form can point at the field at fault. */
class WordDraftException(val problem: WordDraftProblem) : Exception(problem.name)
