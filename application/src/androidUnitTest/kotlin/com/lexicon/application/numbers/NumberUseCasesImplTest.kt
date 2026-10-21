package com.lexicon.application.numbers

import com.lexicon.boundary.NumberProgressBoundary
import com.lexicon.boundary.NumberRepository
import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.numbers.NumberForm
import com.lexicon.interactors.numbers.NumberGender
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
        val rows = mutableMapOf<NumberForm, NumberProgressBoundary>()

        fun row(
            value: Long,
            kind: NumberKind = NumberKind.CARDINAL,
            gender: NumberGender? = null,
            attempted: Int,
            streak: Int,
        ) {
            rows[NumberForm(value, kind, gender)] =
                NumberProgressBoundary(value, kind.name, gender?.name.orEmpty(), attempted, streak, attempted - streak, streak)
        }

        fun streakOf(
            value: Long,
            kind: NumberKind = NumberKind.CARDINAL,
            gender: NumberGender? = null,
        ): Int = rows.getValue(NumberForm(value, kind, gender)).streak

        override suspend fun spec(): String? = spec

        override suspend fun saveSpec(spec: String) {
            this.spec = spec
        }

        override suspend fun progress(): List<NumberProgressBoundary> = rows.values.toList()

        override suspend fun recordAttempt(
            value: Long,
            kind: String,
            gender: String,
            isCorrect: Boolean,
        ) {
            val key = NumberForm(value, NumberKind.valueOf(kind), gender.takeIf { it.isNotEmpty() }?.let(NumberGender::valueOf))
            val old = rows[key] ?: NumberProgressBoundary(value, kind, gender, 0, 0, 0, 0)
            rows[key] = old.copy(
                attempted = old.attempted + 1,
                correct = old.correct + if (isCorrect) 1 else 0,
                incorrect = old.incorrect + if (isCorrect) 0 else 1,
                streak = if (isCorrect) old.streak + 1 else 0,
            )
        }
    }

    private fun cardinal(
        value: Long,
        gender: NumberGender? = null,
    ) = NumberForm(value, NumberKind.CARDINAL, gender)

    private fun ordinal(
        value: Long,
        gender: NumberGender,
    ) = NumberForm(value, NumberKind.ORDINAL, gender)

    private fun listing(value: Long) = NumberForm(value, NumberKind.ENUMERATION)

    private fun genders(form: (NumberGender) -> NumberForm) = NumberGender.entries.map(form)

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
    fun `ordinals, 1 and numbers ending in 2 are practised in all three genders`() {
        assertEquals(
            listOf(cardinal(0)) +
                genders { cardinal(1, it) } + genders { ordinal(1, it) } + listing(1) +
                listOf(cardinal(12)) + genders { ordinal(12, it) } +
                genders { cardinal(22, it) } + genders { ordinal(22, it) } +
                listOf(cardinal(2_000)) +
                listOf(cardinal(2_024)) + genders { ordinal(2_024, it) },
            NumberForm.formsOf(listOf(0, 1, 12, 22, 2_000, 2_024)),
        )
    }

    @Test
    fun `progress counts the forms mastered and tried`() =
        runTest {
            val numbers = FakeNumbers(spec = "3-4")
            numbers.row(3, attempted = 2, streak = 2)
            numbers.row(3, NumberKind.ORDINAL, NumberGender.FEMININE, attempted = 1, streak = 1)
            numbers.row(4, attempted = 1, streak = 0)
            numbers.row(50, attempted = 3, streak = 3)

            val progress = LoadNumberCourseUseCaseImpl(numbers)()!!.progress
            assertEquals(2, progress.items)
            assertEquals(10, progress.total)
            assertEquals(2, progress.mastered)
            assertEquals(3, progress.attempted)
        }

    @Test
    fun `one right answer masters a form and a wrong one takes it back`() =
        runTest {
            val numbers = FakeNumbers(spec = "3")
            val submit = SubmitNumberAnswerUseCaseImpl(numbers)
            val load = LoadNumberCourseUseCaseImpl(numbers)

            submit(NumberQuestion.of(3), "trzy")
            assertEquals(1, load()!!.progress.mastered)

            submit(NumberQuestion.of(3, NumberKind.ORDINAL, NumberGender.FEMININE), "trzecia")
            assertEquals(2, load()!!.progress.mastered)

            submit(NumberQuestion.of(3, NumberKind.ENUMERATION), "po trzecie")
            assertEquals(3, load()!!.progress.mastered)

            submit(NumberQuestion.of(3), "dwa")
            assertEquals(2, load()!!.progress.mastered)
        }

    @Test
    fun `the next form is one not asked yet, not mastered, and tried the fewest times`() =
        runTest {
            val numbers = FakeNumbers(spec = "3-4")
            numbers.row(3, attempted = 2, streak = 2)
            numbers.row(3, NumberKind.ORDINAL, NumberGender.MASCULINE, attempted = 1, streak = 0)
            numbers.row(3, NumberKind.ORDINAL, NumberGender.FEMININE, attempted = 1, streak = 1)
            numbers.row(3, NumberKind.ORDINAL, NumberGender.NEUTER, attempted = 1, streak = 1)
            numbers.row(3, NumberKind.ENUMERATION, attempted = 1, streak = 1)
            numbers.row(4, attempted = 3, streak = 0)
            numbers.row(4, NumberKind.ORDINAL, NumberGender.MASCULINE, attempted = 1, streak = 1)
            numbers.row(4, NumberKind.ORDINAL, NumberGender.FEMININE, attempted = 1, streak = 1)
            numbers.row(4, NumberKind.ENUMERATION, attempted = 1, streak = 1)

            val next = NextNumberUseCaseImpl(numbers)
            assertEquals(ordinal(4, NumberGender.NEUTER), next(random = Random(1))?.form)
            assertEquals(
                ordinal(3, NumberGender.MASCULINE),
                next(asked = setOf(ordinal(4, NumberGender.NEUTER)), random = Random(1))?.form,
            )
        }

    @Test
    fun `when every form was asked the session starts over rather than stopping`() =
        runTest {
            val numbers = FakeNumbers(spec = "8")
            val every = NumberForm.formsOf(listOf(8)).toSet()
            assertEquals(8L, NextNumberUseCaseImpl(numbers)(asked = every)?.value)
        }

    @Test
    fun `an answer is checked against the words of its form and recorded`() =
        runTest {
            val numbers = FakeNumbers(spec = "1-30")
            val submit = SubmitNumberAnswerUseCaseImpl(numbers)
            val feminine = NumberGender.FEMININE

            assertEquals(AnswerVerdict.CORRECT, submit(NumberQuestion.of(21), "Dwadzieścia jeden"))
            assertEquals(AnswerVerdict.ALMOST, submit(NumberQuestion.of(12), "dwanascie"))
            assertEquals(AnswerVerdict.WRONG, submit(NumberQuestion.of(13), "trzy"))
            assertEquals(AnswerVerdict.CORRECT, submit(NumberQuestion.of(1_100), "jeden tysiąc sto"))
            assertEquals(AnswerVerdict.CORRECT, submit(NumberQuestion.of(1, NumberKind.CARDINAL, feminine), "jedna"))
            assertEquals(AnswerVerdict.WRONG, submit(NumberQuestion.of(22, NumberKind.CARDINAL, feminine), "dwadzieścia dwa"))
            assertEquals(AnswerVerdict.CORRECT, submit(NumberQuestion.of(21, NumberKind.ORDINAL, feminine), "dwudziesta pierwsza"))
            assertEquals(AnswerVerdict.WRONG, submit(NumberQuestion.of(2, NumberKind.ORDINAL, NumberGender.NEUTER), "drugi"))
            assertEquals(AnswerVerdict.CORRECT, submit(NumberQuestion.of(3, NumberKind.ENUMERATION), "po trzecie"))

            assertEquals(1, numbers.streakOf(21))
            assertEquals(0, numbers.streakOf(12))
            assertEquals(1, numbers.streakOf(1, NumberKind.CARDINAL, feminine))
            assertEquals(0, numbers.streakOf(22, NumberKind.CARDINAL, feminine))
            assertEquals(1, numbers.streakOf(21, NumberKind.ORDINAL, feminine))
            assertEquals(0, numbers.streakOf(2, NumberKind.ORDINAL, NumberGender.NEUTER))
            assertEquals(1, numbers.streakOf(3, NumberKind.ENUMERATION))
        }
}
