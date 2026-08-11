package com.lexicon.domain.course

import com.lexicon.domain.dictation.AnswerNormalizer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckExerciseAnswerUseCaseImplTest {
    private val useCase = CheckExerciseAnswerUseCaseImpl(AnswerNormalizer())

    @Test
    fun `exact match is correct`() {
        assertTrue(useCase("kot", "kot"))
    }

    @Test
    fun `case and surrounding whitespace are ignored, same as the trainings`() {
        assertTrue(useCase("Kot", "  kot  "))
    }

    @Test
    fun `different text is incorrect`() {
        assertFalse(useCase("kot", "pies"))
    }

    @Test
    fun `empty submission is incorrect`() {
        assertFalse(useCase("kot", ""))
    }
}
