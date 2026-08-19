package com.lexicon.model.training

enum class StepOutcome {
    CORRECT,
    INCORRECT,
    SKIPPED,

    SEEN,
    ;

    val isCorrect: Boolean get() = this == CORRECT

    val countsAsAnswered: Boolean get() = this != SEEN
}
