package com.lexicon.interactors.program

enum class ProgramDraftProblem {
    MISSING_TITLE,

    NO_TRAININGS,

    EMPTY_STUDY_SET,
}

class ProgramDraftException(val problem: ProgramDraftProblem) : Exception(problem.name)
