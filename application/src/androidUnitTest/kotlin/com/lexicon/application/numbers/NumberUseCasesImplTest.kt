package com.lexicon.application.numbers

import com.lexicon.boundary.NumberProgressBoundary
import com.lexicon.boundary.NumberRepository
import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.numbers.NumberQuestion
import com.lexicon.interactors.numbers.NumberSelection
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class NumberUseCasesImplTest {
    private class FakeNumbers(
        var spec: String? = null,
    ) : NumberRepository {
        val rows = mutableMapOf<Long, NumberProgressBoundary>()

        override suspend fun spec(): String? = spec

        override suspend fun saveSpec(spec: String) {
            this.spec = spec
        }

        override suspend fun progress(): List<NumberProgressBoundary> = rows.values.toList()

        override suspend fun recordAttempt(
            value: Long,
            isCorrect: Boolean,
        ) {
            val old = rows[value] ?: NumberProgressBoundary(value, 0, 0, 0, 0)
            rows[value] = old.copy(
                attempted = old.attempted + 1,
                correct = old.correct + if (isCorrect) 1 else 0,
                incorrect = old.incorrect + if (isCorrect) 0 else 1,
                streak = if (isCorrect) old.streak + 1 else 0,
            )
        }
    }

    @Test
    fun `there is no course until numbers are chosen`() =
        runTest {
            val numbers = FakeNumbers()
            assertNull(LoadNumberCourseUseCaseImpl(numbers)())
            assertNull(NextNumberUseCaseImpl(numbers)())
        }

    @Test
    fun `only a readable choice is saved`() =
        runTest {
            val numbers = FakeNumbers(spec = "1-5")
            assertEquals(NumberSelection.Unreadable("x"), SaveNumberCourseUseCaseImpl(numbers)("x"))
            assertEquals("1-5", numbers.spec)
            assertEquals(NumberSelection.Valid(listOf(7L, 8L)), SaveNumberCourseUseCaseImpl(numbers)(" 7-8 "))
            assertEquals("7-8", numbers.spec)
        }

    @Test
    fun `progress counts the chosen numbers mastered and tried`() =
        runTest {
            val numbers = FakeNumbers(spec = "1-4")
            numbers.rows[1] = NumberProgressBoundary(1, attempted = 2, correct = 2, incorrect = 0, streak = 2)
            numbers.rows[2] = NumberProgressBoundary(2, attempted = 1, correct = 0, incorrect = 1, streak = 0)
            numbers.rows[50] = NumberProgressBoundary(50, attempted = 3, correct = 3, incorrect = 0, streak = 3)

            val progress = LoadNumberCourseUseCaseImpl(numbers)()!!.progress
            assertEquals(4, progress.total)
            assertEquals(1, progress.mastered)
            assertEquals(2, progress.attempted)
        }

    @Test
    fun `the next number is one not asked yet, not mastered, and tried the fewest times`() =
        runTest {
            val numbers = FakeNumbers(spec = "1-4")
            numbers.rows[1] = NumberProgressBoundary(1, attempted = 2, correct = 2, incorrect = 0, streak = 2)
            numbers.rows[2] = NumberProgressBoundary(2, attempted = 1, correct = 0, incorrect = 1, streak = 0)
            numbers.rows[3] = NumberProgressBoundary(3, attempted = 3, correct = 1, incorrect = 2, streak = 0)

            val next = NextNumberUseCaseImpl(numbers)
            assertEquals(4L, next(random = Random(1))?.value)
            assertEquals(2L, next(asked = setOf(4), random = Random(1))?.value)
        }

    @Test
    fun `when every number was asked the session starts over rather than stopping`() =
        runTest {
            val numbers = FakeNumbers(spec = "8")
            assertEquals(8L, NextNumberUseCaseImpl(numbers)(asked = setOf(8))?.value)
        }

    @Test
    fun `an answer is checked against the words and recorded`() =
        runTest {
            val numbers = FakeNumbers(spec = "1-30")
            val submit = SubmitNumberAnswerUseCaseImpl(numbers)

            assertEquals(AnswerVerdict.CORRECT, submit(NumberQuestion.of(21), "Dwadzieścia jeden"))
            assertEquals(AnswerVerdict.ALMOST, submit(NumberQuestion.of(12), "dwanascie"))
            assertEquals(AnswerVerdict.WRONG, submit(NumberQuestion.of(13), "trzy"))
            assertEquals(AnswerVerdict.CORRECT, submit(NumberQuestion.of(1_100), "jeden tysiąc sto"))

            assertEquals(1, numbers.rows.getValue(21).streak)
            assertEquals(0, numbers.rows.getValue(12).streak)
            assertEquals(1, numbers.rows.getValue(13).incorrect)
        }
}
