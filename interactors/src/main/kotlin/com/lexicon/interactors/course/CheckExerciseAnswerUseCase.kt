package com.lexicon.interactors.course

interface CheckExerciseAnswerUseCase {
    /**
     * Whether what the learner wrote or picked counts as the expected answer.
     * Comparison is the same one the trainings use, so trailing spaces and
     * capitalisation do not fail a correct answer.
     */
    operator fun invoke(
        expected: String,
        submitted: String,
    ): Boolean
}
