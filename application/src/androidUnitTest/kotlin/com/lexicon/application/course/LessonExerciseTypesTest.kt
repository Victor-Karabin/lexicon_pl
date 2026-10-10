package com.lexicon.application.course

import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.course.LessonSession
import com.lexicon.model.course.AnswerMatch
import com.lexicon.model.course.ChoiceQuestion
import com.lexicon.model.course.FormGroup
import com.lexicon.model.course.LessonId
import com.lexicon.model.course.LessonQuestion
import com.lexicon.model.course.LessonScreen
import com.lexicon.model.course.LessonScript
import com.lexicon.model.course.LessonStep
import com.lexicon.model.course.OrderLine
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonExerciseTypesTest {
    private fun question(
        key: String,
        vararg answers: String,
        match: AnswerMatch = AnswerMatch.WHOLE,
    ) = LessonQuestion(key, key, answers.toList().toImmutableList(), null, match = match)

    private val halves = LessonScreen.Choice(
        id = "2D",
        title = "Match the halves",
        instruction = "",
        tracks = persistentListOf(),
        transcript = null,
        items = listOf("d", "e", "b", "f", "a", "c").mapIndexed { index, answer ->
            ChoiceQuestion(question(index.toString(), answer), persistentListOf("a", "b", "c", "d", "e", "f"))
        }.toImmutableList(),
        interchangeable = persistentListOf(persistentListOf("0", "2")),
    )

    private val order = LessonScreen.Ordering(
        id = "2E",
        title = "Put the dialogue in order",
        instruction = "",
        tracks = persistentListOf(),
        transcript = null,
        lines = listOf("a", "b", "c").map { OrderLine(it, null, it) }.toImmutableList(),
        questions = persistentListOf(question("a", "3"), question("b", "1"), question("c", "2")),
    )

    private val form = LessonScreen.Form(
        id = "7C",
        title = "Questionnaire",
        instruction = "",
        tracks = persistentListOf(),
        transcript = null,
        groups = persistentListOf(
            FormGroup(
                "Uwe",
                persistentListOf(
                    question("0-0", "z Niemiec", "Niemiec", "Niemcy", match = AnswerMatch.KEY_WORDS),
                    question("0-1", "ul. Floriańska", "Floriańska", match = AnswerMatch.KEY_WORDS),
                    question("0-2", "0 601 15 26 10", match = AnswerMatch.KEY_WORDS),
                    question("0-3", "żeby pracować w Warszawie", "pracować w Warszawie", match = AnswerMatch.KEY_WORDS),
                ),
            ),
        ),
    )

    private fun session(screen: LessonScreen) =
        LessonSession(
            LessonScript(
                LessonId("krok-a1-02"),
                persistentListOf(LessonStep("1", "Step", persistentListOf(screen.id))),
                persistentListOf(screen),
            ),
        )

    private fun LessonSession.answering(vararg values: Pair<String, String>) =
        values.fold(
            this,
        ) { session, (key, value) -> session.withAnswer(key, value) }

    @Test
    fun `two items that start alike accept their endings in either order`() {
        val swapped = session(halves).answering("0" to "b", "1" to "e", "2" to "d", "3" to "f", "4" to "a", "5" to "c").check()
        assertEquals(6, swapped.screenScore(halves).correct)
        val asPrinted = session(halves).answering("0" to "d", "1" to "e", "2" to "b", "3" to "f", "4" to "a", "5" to "c").check()
        assertEquals(6, asPrinted.screenScore(halves).correct)
    }

    @Test
    fun `the same ending twice counts once and the other item is shown the ending it still needs`() {
        val twice = session(halves).answering("0" to "d", "1" to "e", "2" to "d", "3" to "f", "4" to "a", "5" to "c").check()
        assertEquals(AnswerVerdict.CORRECT, twice.verdict("2D", "0"))
        assertEquals(AnswerVerdict.WRONG, twice.verdict("2D", "2"))
        assertEquals("b", twice.expectedAnswer("2D", "2"))
        assertEquals(5, twice.screenScore(halves).correct)
    }

    @Test
    fun `tapping lines numbers them in tap order and tapping again frees the number`() {
        var ordering = session(order).withPositionToggled("b").withPositionToggled("c")
        assertEquals("1", ordering.answer("2E", "b"))
        assertEquals("2", ordering.answer("2E", "c"))
        assertFalse(ordering.canCheck)

        ordering = ordering.withPositionToggled("b")
        assertEquals("", ordering.answer("2E", "b"))
        ordering = ordering.withPositionToggled("a")
        assertEquals("1", ordering.answer("2E", "a"))
        ordering = ordering.withPositionToggled("b")
        assertEquals("3", ordering.answer("2E", "b"))
        assertTrue(ordering.canCheck)

        val checked = ordering.check()
        assertEquals(AnswerVerdict.WRONG, checked.verdict("2E", "a"))
        assertEquals("3", checked.expectedAnswer("2E", "a"))
        assertEquals(AnswerVerdict.CORRECT, checked.verdict("2E", "c"))
        assertEquals(checked, checked.withPositionToggled("a"))
    }

    @Test
    fun `a questionnaire answer is right when it holds the key words`() {
        val filled = session(form).answering(
            "0-0" to "Jest z Niemiec.",
            "0-1" to "Floriańska",
            "0-2" to "0601152610",
            "0-3" to "bo chce pracować w Warszawie",
        ).check()
        listOf("0-0", "0-1", "0-2", "0-3").forEach { assertEquals(it, AnswerVerdict.CORRECT, filled.verdict("7C", it)) }
    }

    @Test
    fun `a questionnaire answer without the key words is wrong and one missing only accents is almost`() {
        val filled = session(form).answering(
            "0-0" to "z Hiszpanii",
            "0-1" to "Florianska",
            "0-2" to "601 15 26 10",
            "0-3" to "pracować",
        ).check()
        assertEquals(AnswerVerdict.WRONG, filled.verdict("7C", "0-0"))
        assertEquals("z Niemiec", filled.expectedAnswer("7C", "0-0"))
        assertEquals(AnswerVerdict.ALMOST, filled.verdict("7C", "0-1"))
        assertEquals(AnswerVerdict.WRONG, filled.verdict("7C", "0-2"))
        assertEquals(AnswerVerdict.WRONG, filled.verdict("7C", "0-3"))
    }
}
