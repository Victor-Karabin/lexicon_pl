package com.lexicon.domain.course

import com.lexicon.domain.dictation.AnswerNormalizer
import com.lexicon.interactors.course.CheckExerciseAnswerUseCase
import javax.inject.Inject

class CheckExerciseAnswerUseCaseImpl
    @Inject
    constructor(
        private val normalizer: AnswerNormalizer,
    ) : CheckExerciseAnswerUseCase {
        override fun invoke(
            expected: String,
            submitted: String,
        ): Boolean = normalizer.matches(expected, submitted)
    }
