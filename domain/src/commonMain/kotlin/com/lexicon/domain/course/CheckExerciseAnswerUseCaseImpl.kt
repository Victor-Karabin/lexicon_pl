package com.lexicon.domain.course

import com.lexicon.domain.dictation.AnswerNormalizer
import com.lexicon.interactors.course.CheckExerciseAnswerUseCase

class CheckExerciseAnswerUseCaseImpl(
    private val normalizer: AnswerNormalizer,
) : CheckExerciseAnswerUseCase {
    override fun invoke(
        expected: String,
        submitted: String,
    ): Boolean = normalizer.matches(expected, submitted)
}
