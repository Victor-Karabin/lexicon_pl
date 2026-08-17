package com.lexicon.interactors.program

/** What is wrong with a draft, in the order the form should complain about it. */
enum class ProgramDraftProblem {
    MISSING_TITLE,

    /** A day with no training in it has nothing to do. */
    NO_TRAININGS,

    /** The study set is what the program teaches, so an empty one teaches nothing. */
    NO_FAVOURITES,
}

/** Raised by [CreateProgramUseCase] so the form can point at the field at fault. */
class ProgramDraftException(val problem: ProgramDraftProblem) : Exception(problem.name)
