package com.lexicon.application.numbers

import com.lexicon.boundary.NumberProgressBoundary
import com.lexicon.boundary.NumberRepository
import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.numbers.NumberForm
import com.lexicon.interactors.numbers.NumberKind
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
        val rows = mutableMapOf<Pair<Long, NumberKind>, NumberProgressBoundary>()

        fun row(
            value: Long,
            kind: NumberKind = NumberKind.CARDINAL,
            attempted: Int,
            streak: Int,
        ) {
            rows[value to kind] = NumberProgressBoundary(value, kind.name, attempted, streak, attempted - streak, streak)
        }

        fun streakOf(
            value: Long,
            kind: NumberKind = NumberKind.CARDINAL,
        ): Int = rows.getValue(value to kind).streak

        override suspend fun spec(): String? = spec

        override suspend fun saveSpec(spec: String) {
            this.spec = spec
        }

        override suspend fun progress(): List<NumberProgressBoundary> = rows.values.toList()

        override suspend fun recordAttempt(
            value: Long,
            kind: String,
            isCorrect: Boolean,
        ) {
            val key = value to NumberKind.valueOf(kind)
            val old = rows[key] ?: NumberProgressBoundary(value, kind, 0, 0, 0, 0)
            rows[key] = old.copy(
                attempted = old.attempted + 1,
                correct = old.correct + if (isCorrect) 1 else 0,
                incorrect = old.incorrect + if (isCorrect) 0 else 1,
                streak = if (isCorrect) old.streak + 1 else 0,
            )
        }
    }

    private fun cardinal(value: Long) = NumberForm(value, NumberKind.CARDINAL)

    private fun ordinal(value: Long) = NumberForm(value, NumberKind.ORDINAL)

    private fun listing(value: Long) = NumberForm(value, NumberKind.ENUMERATION)

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
    fun `every number is practised as a cardinal and, where it has them, as an ordinal and a list point`() {
        assertEquals(
            listOf(
                cardinal(0),
                cardinal(1),
                ordinal(1),
                listing(1),
                cardinal(1_000),
                ordinal(1_000),
                cardinal(2_000),
                cardinal(2_024),
                ordinal(2_024),
            ),
            NumberForm.formsOf(listOf(0, 1, 1_000, 2_000, 2_024)),
        )
    }

    @Test
    fun `progress counts the forms mastered and tried`() =
        runTest {
            val numbers = FakeNumbers(spec = "1-4")
            numbers.row(1, attempted = 2, streak = 2)
            numbers.row(1, NumberKind.ORDINAL, attempted = 1, streak = 1)
            numbers.row(2, attempted = 1, streak = 0)
            numbers.row(50, attempted = 3, streak = 3)

            val progress = LoadNumberCourseUseCaseImpl(numbers)()!!.progress
            assertEquals(4, progress.numbers)
            assertEquals(12, progress.total)
            assertEquals(2, progress.mastered)
            assertEquals(3, progress.attempted)
        }

    @Test
    fun `one right answer masters a form and a wrong one takes it back`() =
        runTest {
            val numbers = FakeNumbers(spec = "1-2")
            val submit = SubmitNumberAnswerUseCaseImpl(numbers)
            val load = LoadNumberCourseUseCaseImpl(numbers)

            submit(NumberQuestion.of(1), "jeden")
            assertEquals(1, load()!!.progress.mastered)

            submit(NumberQuestion.of(1, NumberKind.ORDINAL), "pierwszy")
            assertEquals(2, load()!!.progress.mastered)

            submit(NumberQuestion.of(1, NumberKind.ENUMERATION), "po pierwsze")
            assertEquals(3, load()!!.progress.mastered)

            submit(NumberQuestion.of(1), "dwa")
            assertEquals(2, load()!!.progress.mastered)
        }

    @Test
    fun `the next form is one not asked yet, not mastered, and tried the fewest times`() =
        runTest {
            val numbers = FakeNumbers(spec = "1-2")
            numbers.row(1, attempted = 2, streak = 2)
            numbers.row(1, NumberKind.ORDINAL, attempted = 1, streak = 0)
            numbers.row(1, NumberKind.ENUMERATION, attempted = 1, streak = 1)
            numbers.row(2, attempted = 3, streak = 0)
            numbers.row(2, NumberKind.ENUMERATION, attempted = 1, streak = 1)

            val next = NextNumberUseCaseImpl(numbers)
            assertEquals(ordinal(2), next(random = Random(1))?.form)
            assertEquals(ordinal(1), next(asked = setOf(ordinal(2)), random = Random(1))?.form)
        }

    @Test
    fun `when every form was asked the session starts over rather than stopping`() =
        runTest {
            val numbers = FakeNumbers(spec = "8")
            assertEquals(8L, NextNumberUseCaseImpl(numbers)(asked = setOf(cardinal(8), ordinal(8), listing(8)))?.value)
        }

    @Test
    fun `an answer is checked against the words of its form and recorded`() =
        runTest {
            val numbers = FakeNumbers(spec = "1-30")
            val submit = SubmitNumberAnswerUseCaseImpl(numbers)

            assertEquals(AnswerVerdict.CORRECT, submit(NumberQuestion.of(21), "Dwadzieścia jeden"))
            assertEquals(AnswerVerdict.ALMOST, submit(NumberQuestion.of(12), "dwanascie"))
            assertEquals(AnswerVerdict.WRONG, submit(NumberQuestion.of(13), "trzy"))
            assertEquals(AnswerVerdict.CORRECT, submit(NumberQuestion.of(1_100), "jeden tysiąc sto"))
            assertEquals(AnswerVerdict.CORRECT, submit(NumberQuestion.of(21, NumberKind.ORDINAL), "dwudziesty pierwszy"))
            assertEquals(AnswerVerdict.WRONG, submit(NumberQuestion.of(2, NumberKind.ORDINAL), "dwa"))
            assertEquals(AnswerVerdict.CORRECT, submit(NumberQuestion.of(3, NumberKind.ENUMERATION), "po trzecie"))
            assertEquals(AnswerVerdict.WRONG, submit(NumberQuestion.of(4, NumberKind.ENUMERATION), "po czwarty"))

            assertEquals(1, numbers.streakOf(21))
            assertEquals(0, numbers.streakOf(12))
            assertEquals(1, numbers.streakOf(21, NumberKind.ORDINAL))
            assertEquals(0, numbers.streakOf(2, NumberKind.ORDINAL))
            assertEquals(1, numbers.streakOf(3, NumberKind.ENUMERATION))
            assertEquals(0, numbers.streakOf(4, NumberKind.ENUMERATION))
        }
}
