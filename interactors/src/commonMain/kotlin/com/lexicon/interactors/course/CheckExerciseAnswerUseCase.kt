package com.lexicon.interactors.course

interface CheckExerciseAnswerUseCase {
    operator fun invoke(
        expected: String,
        submitted: String,
    ): Boolean
}
