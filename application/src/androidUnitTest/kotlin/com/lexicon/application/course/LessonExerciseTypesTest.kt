package com.lexicon.application.course

import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.course.LessonSession
import com.lexicon.model.course.AnswerKeyboard
import com.lexicon.model.course.AnswerMatch
import com.lexicon.model.course.ChoiceQuestion
import com.lexicon.model.course.FormGroup
import com.lexicon.model.course.GapLine
import com.lexicon.model.course.GapSection
import com.lexicon.model.course.InlineChoiceLine
import com.lexicon.model.course.LessonId
import com.lexicon.model.course.LessonPhrase
import com.lexicon.model.course.LessonQuestion
import com.lexicon.model.course.LessonScreen
import com.lexicon.model.course.LessonScript
import com.lexicon.model.course.LessonStep
import com.lexicon.model.course.NewWords
import com.lexicon.model.course.OrderLine
import com.lexicon.model.course.TranscriptUnlock
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun `a variant heard by ear counts as right and shows the usual spelling`() {
        val name = LessonQuestion("1", "1", persistentListOf("Thomas"), null, variants = persistentListOf("Tomas"))
        val form = LessonScreen.Write("6B", "", "", persistentListOf(), null, AnswerKeyboard.TEXT, persistentListOf(name))
        val heard = session(form).withAnswer("1", "tomas").check()
        assertEquals(AnswerVerdict.CORRECT, heard.verdict("6B", "1"))
        assertEquals("Thomas", heard.usualForm("6B", "1"))
        assertNull(session(form).withAnswer("1", "Thomas").check().usualForm("6B", "1"))
        assertNull(session(form).withAnswer("1", "Tom").check().usualForm("6B", "1"))
    }

    @Test
    fun `new words that give the answers away open only after the check`() {
        val words = NewWords(persistentListOf(LessonPhrase("okno", "window")), TranscriptUnlock.AfterCheck)
        val script = LessonScript(
            LessonId("krok-a1-01"),
            persistentListOf(LessonStep("1", "Step", persistentListOf("2E"))),
            persistentListOf(order),
            persistentMapOf("2E" to words),
        )
        val start = LessonSession(script)
        assertFalse(start.isNewWordsUnlocked(order))
        val checked = start.withPositionToggled("b").withPositionToggled("c").withPositionToggled("a").check()
        assertTrue(checked.isNewWordsUnlocked(order))
    }

    @Test
    fun `options keep one order during a visit, shuffled once per screen when the items share them`() {
        val pronouns = persistentListOf("on", "ona", "ono", "oni", "one")
        val choice = LessonScreen.Choice(
            id = "3C",
            title = "",
            instruction = "",
            tracks = persistentListOf(),
            transcript = null,
            items = (0..5).map { ChoiceQuestion(question(it.toString(), "ona"), pronouns) }.toImmutableList(),
        )
        val shown = choice.items.map { session(choice).optionsOf(choice, it, visit = 7) }
        assertTrue(shown.all { it == shown.first() })
        assertEquals(pronouns.toSet(), shown.first().toSet())
        assertEquals(shown, choice.items.map { session(choice).optionsOf(choice, it, visit = 7) })

        assertEquals(pronouns, session(choice).optionsOf(choice.copy(shuffle = false), choice.items.first(), visit = 7))
        assertEquals(
            listOf("a", "b", "c", "d", "e", "f"),
            session(halves).optionsOf(halves.copy(legend = persistentListOf("a. pani")), halves.items.first(), visit = 7),
        )
    }

    @Test
    fun `the two words of a pair swap places between visits`() {
        val pair = ChoiceQuestion(question("0", "krzywy"), persistentListOf("krzywy", "grzyby"))
        val other = ChoiceQuestion(question("1", "cześć"), persistentListOf("sześć", "cześć"))
        val pairs = LessonScreen.Choice("1C", "", "", persistentListOf(), null, persistentListOf(pair, other))
        val orders = (1..20).map { visit -> session(pairs).optionsOf(pairs, pair, visit) }.toSet()
        assertEquals(setOf(listOf("krzywy", "grzyby"), listOf("grzyby", "krzywy")), orders)
    }

    @Test
    fun `both choices of an inline sentence are graded on their own`() {
        val line = InlineChoiceLine(
            label = "a",
            text = "[1] klucz jest [2]",
            groups = persistentListOf(
                ChoiceQuestion(question("a-1", "Ten"), persistentListOf("Ten", "Ta", "To")),
                ChoiceQuestion(question("a-2", "nowy"), persistentListOf("nowy", "nowa", "nowe")),
            ),
        )
        val inline = LessonScreen.InlineChoice("5D", "", "", persistentListOf(), null, persistentListOf(line))
        assertFalse(session(inline).withAnswer("a-1", "Ten").canCheck)
        val checked = session(inline).answering("a-1" to "Ten", "a-2" to "nowa").check()
        assertEquals(AnswerVerdict.CORRECT, checked.verdict("5D", "a-1"))
        assertEquals(AnswerVerdict.WRONG, checked.verdict("5D", "a-2"))
        assertEquals(1, checked.screenScore(inline).correct)
    }

    @Test
    fun `gap-fill adjectives of one gender may swap nouns, but one used twice counts once`() {
        val pairs = LessonScreen.GapFill(
            id = "6B",
            title = "",
            instruction = "",
            tracks = persistentListOf(),
            transcript = null,
            sections = persistentListOf(),
            questions = persistentListOf(question("1", "inteligentny"), question("4", "sympatyczny"), question("7", "duży")),
            interchangeable = persistentListOf(persistentListOf("1", "4", "7")),
        )
        val swapped = session(pairs).answering("1" to "duży", "4" to "inteligentny", "7" to "sympatyczny").check()
        assertEquals(3, swapped.screenScore(pairs).correct)
        val twice = session(pairs).answering("1" to "duży", "4" to "duży", "7" to "sympatyczny").check()
        assertEquals(2, twice.screenScore(pairs).correct)
        assertEquals("inteligentny", twice.expectedAnswer("6B", "4"))
    }

    @Test
    fun `a gap fill with a picture choice grades the choice and the gaps together`() {
        val pictures = persistentListOf("📷 photo album", "📖 book", "🥟 dough", "💻 laptop")
        val choice = ChoiceQuestion(question("choice-1", "🥟 dough"), pictures)
        val dialogue = LessonScreen.GapFill(
            id = "1A",
            title = "",
            instruction = "",
            tracks = persistentListOf(),
            transcript = null,
            sections = persistentListOf(GapSection(null, null, persistentListOf(GapLine("Pani Joanna", "[1] pierogi. A ty [2] gotować?")))),
            questions = persistentListOf(choice.question, question("1", "Gotuję"), question("2", "umiesz")),
            choices = persistentListOf(choice),
        )
        val checked = session(dialogue).answering("choice-1" to "📖 book", "1" to "gotuję", "2" to "umiesz").check()
        assertEquals(AnswerVerdict.WRONG, checked.verdict("1A", "choice-1"))
        assertEquals(AnswerVerdict.CORRECT, checked.verdict("1A", "1"))
        assertEquals(2, checked.screenScore(dialogue).correct)
        assertEquals(3, checked.screenScore(dialogue).total)
    }

    @Test
    fun `on a screen that uses each answer once, a word typed twice is wrong the second time`() {
        val pairs = LessonScreen.GapFill(
            id = "1C",
            title = "",
            instruction = "",
            tracks = persistentListOf(),
            transcript = null,
            sections = persistentListOf(GapSection(null, null, persistentListOf(GapLine(null, "czytać [1], [2] · lubić [3], [4]")))),
            questions = persistentListOf(
                question("1", "gazetę", "książkę", "list"),
                question("2", "książkę", "gazetę", "list"),
                question("3", "piwo", "kawę", "list", "gazetę"),
                question("4", "kawę", "piwo", "list", "gazetę"),
            ),
            eachAnswerOnce = true,
        )
        val checked = session(pairs).answering("1" to "gazetę", "2" to "list", "3" to "gazetę", "4" to "kawę").check()
        assertEquals(AnswerVerdict.CORRECT, checked.verdict("1C", "1"))
        assertEquals(AnswerVerdict.CORRECT, checked.verdict("1C", "2"))
        assertEquals(AnswerVerdict.WRONG, checked.verdict("1C", "3"))
        assertEquals(AnswerVerdict.CORRECT, checked.verdict("1C", "4"))
    }

    @Test
    fun `a choice item with two right options accepts either`() {
        val endings = LessonScreen.Choice(
            id = "2C",
            title = "",
            instruction = "",
            tracks = persistentListOf(),
            transcript = null,
            items = persistentListOf(
                ChoiceQuestion(question("0", "-naście", "-dzieści"), persistentListOf("-naście", "-dzieści", "-dziesiąt")),
            ),
        )
        assertEquals(AnswerVerdict.CORRECT, session(endings).answering("0" to "-dzieści").check().verdict("2C", "0"))
        assertEquals(AnswerVerdict.CORRECT, session(endings).answering("0" to "-naście").check().verdict("2C", "0"))
        assertEquals(AnswerVerdict.WRONG, session(endings).answering("0" to "-dziesiąt").check().verdict("2C", "0"))
    }

    @Test
    fun `any masculine adjective fits any man once, while the professions and the feminine adjective are fixed`() {
        val people = LessonScreen.GapFill(
            id = "4E",
            title = "",
            instruction = "",
            tracks = persistentListOf(),
            transcript = null,
            sections = persistentListOf(),
            questions = persistentListOf(
                question("1", "zdolna"),
                question("2", "aktorka"),
                question("3", "atrakcyjny"),
                question("4", "aktor"),
                question("5", "dobry"),
                question("6", "malarz"),
                question("7", "utalentowany"),
                question("8", "polityk"),
            ),
            interchangeable = persistentListOf(persistentListOf("3", "5", "7")),
        )
        val swapped = session(people).answering(
            "1" to "zdolna",
            "2" to "aktorka",
            "3" to "dobry",
            "4" to "aktor",
            "5" to "utalentowany",
            "6" to "malarz",
            "7" to "atrakcyjny",
            "8" to "polityk",
        ).check()
        assertEquals(8, swapped.screenScore(people).correct)
        val twice = session(people).answering(
            "1" to "zdolna",
            "2" to "aktor",
            "3" to "dobry",
            "4" to "aktorka",
            "5" to "dobry",
            "6" to "malarz",
            "7" to "zdolna",
            "8" to "polityk",
        ).check()
        assertEquals(AnswerVerdict.CORRECT, twice.verdict("4E", "3"))
        assertEquals(AnswerVerdict.WRONG, twice.verdict("4E", "5"))
        assertEquals(AnswerVerdict.WRONG, twice.verdict("4E", "7"))
        assertEquals(AnswerVerdict.WRONG, twice.verdict("4E", "2"))
        assertEquals(4, twice.screenScore(people).correct)
    }

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
