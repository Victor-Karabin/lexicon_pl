package com.lexicon.interactors.program

enum class ProgramDraftProblem {
    MISSING_TITLE,

    NO_TRAININGS,

    NO_FAVOURITES,
}

class ProgramDraftException(val problem: ProgramDraftProblem) : Exception(problem.name)
