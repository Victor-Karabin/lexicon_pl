package com.lexicon.application.course

import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.course.LessonAnswerChecker
import com.lexicon.model.course.AnswerMatch
import org.junit.Assert.assertEquals
import org.junit.Test

class LessonAnswerCheckerTest {
    private fun verdict(
        expected: String,
        given: String,
    ) = LessonAnswerChecker.verdict(listOf(expected), given)

    @Test
    fun `every key of lesson 1 is accepted as written`() {
        val keys = listOf(
            "cześć", "szkoła", "dziękuję", "przepraszam", "powtórzyć", "sześć", "dziewięć", "gdzie",
            "dobry", "Nazywam", "Bardzo", "mi", "jestem", "Cześć", "Miło", "Mnie",
            "Jestem", "nie", "nazywa", "się",
            "0", "2", "6", "9", "10", "1", "5", "4",
            "trzy", "siedem", "osiem", "zero", "jeden",
        )
        keys.forEach { assertEquals(it, AnswerVerdict.CORRECT, verdict(it, it)) }
    }

    @Test
    fun `letter case and surrounding spaces are ignored`() {
        assertEquals(AnswerVerdict.CORRECT, verdict("Nazywam", "nazywam"))
        assertEquals(AnswerVerdict.CORRECT, verdict("dobry", "  DOBRY "))
        assertEquals(AnswerVerdict.CORRECT, verdict("Cześć", "CZEŚĆ"))
    }

    @Test
    fun `a capitalised key accepts the word in lower case and the other way round`() {
        listOf("Nazywam", "Bardzo", "Cześć", "Miło", "Mnie", "Jestem").forEach { key ->
            assertEquals(key, AnswerVerdict.CORRECT, verdict(key, key.lowercase()))
            assertEquals(key, AnswerVerdict.CORRECT, verdict(key.lowercase(), key))
        }
        assertEquals(AnswerVerdict.ALMOST, verdict("Cześć", "czesc"))
    }

    @Test
    fun `punctuation around the answer is ignored`() {
        assertEquals(AnswerVerdict.CORRECT, verdict("Cześć", "Cześć!"))
        assertEquals(AnswerVerdict.CORRECT, verdict("dobry", "dobry."))
        assertEquals(AnswerVerdict.CORRECT, verdict("Mnie", "„Mnie”"))
    }

    @Test
    fun `a letter typed as a base letter and a separate accent counts as the Polish letter`() {
        assertEquals(AnswerVerdict.CORRECT, verdict("cześć", "cześć"))
        assertEquals(AnswerVerdict.CORRECT, verdict("dziękuję", "dziękuję"))
        assertEquals(AnswerVerdict.CORRECT, verdict("może", "może"))
    }

    @Test
    fun `missing diacritics are wrong but almost`() {
        assertEquals(AnswerVerdict.ALMOST, verdict("sześć", "szesc"))
        assertEquals(AnswerVerdict.ALMOST, verdict("dziękuję", "dziekuje"))
        assertEquals(AnswerVerdict.ALMOST, verdict("szkoła", "szkola"))
        assertEquals(AnswerVerdict.ALMOST, verdict("się", "sie"))
    }

    @Test
    fun `a different word is wrong, not almost`() {
        assertEquals(AnswerVerdict.WRONG, verdict("sześć", "cześć"))
        assertEquals(AnswerVerdict.WRONG, verdict("dziewięć", "dzięwięć"))
        assertEquals(AnswerVerdict.WRONG, verdict("sześć", "seść"))
        assertEquals(AnswerVerdict.WRONG, verdict("9", "10"))
        assertEquals(AnswerVerdict.WRONG, verdict("Mnie", "mi"))
    }

    @Test
    fun `a wrong diacritic is not forgiven as almost when the plain letters differ`() {
        assertEquals(AnswerVerdict.WRONG, verdict("powtórzyć", "powturzyć"))
    }

    @Test
    fun `nothing typed is empty rather than wrong`() {
        assertEquals(AnswerVerdict.EMPTY, verdict("trzy", "   "))
    }

    @Test
    fun `any accepted answer counts`() {
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(listOf("dzień dobry", "dobry"), "Dobry"))
    }

    @Test
    fun `sentence building ignores case and punctuation and accepts alternatives`() {
        val toilet = listOf("Przepraszam, gdzie jest toaleta?")
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(toilet, "przepraszam gdzie jest toaleta"))
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(toilet, "Przepraszam,gdzie jest toaleta ?"))
        assertEquals(AnswerVerdict.WRONG, LessonAnswerChecker.verdict(toilet, "Gdzie jest toaleta, przepraszam?"))
        val coffee = listOf("Kawa i cukier są tam.", "Cukier i kawa są tam.", "Tam są kawa i cukier.")
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(coffee, "tam są kawa i cukier"))
        assertEquals(AnswerVerdict.ALMOST, LessonAnswerChecker.verdict(coffee, "Kawa i cukier sa tam"))
        assertEquals(
            AnswerVerdict.CORRECT,
            LessonAnswerChecker.verdict(listOf("Dzień dobry. Jestem Maria Benini."), "dzień dobry jestem maria benini"),
        )
    }

    @Test
    fun `phone numbers ignore spaces`() {
        assertEquals(AnswerVerdict.CORRECT, verdict("0607234599", "060 723 45 99"))
        assertEquals(AnswerVerdict.CORRECT, verdict("607 44 32 89", "607443289"))
        assertEquals(AnswerVerdict.WRONG, verdict("0607234599", "0607234598"))
    }

    @Test
    fun `gap answers with two words and a full stop accept the plain words`() {
        assertEquals(AnswerVerdict.CORRECT, verdict("Proszę. Do", "proszę do"))
        assertEquals(AnswerVerdict.ALMOST, verdict("Proszę. Do", "Prosze. Do"))
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(listOf("Thomas", "Tomas"), "Tomas"))
        assertEquals(AnswerVerdict.CORRECT, verdict("ulica Studencka 3", "Ulica Studencka 3"))
        assertEquals(AnswerVerdict.WRONG, verdict("ulica Studencka 3", "ulica Studentska 3"))
    }

    @Test
    fun `the new keys of lesson 1 are accepted as written`() {
        listOf(
            "wolne", "jest", "Na", "okno", "Proszę", "napisać", "przeliterować", "przeczytać", "znaczy", "mówi", "powtórzyć",
            "rozumiem", "otworzyć", "to", "Jestem Niemcem", "bank", "fotograf", "plan", "teatr", "interesujący", "aktor",
            "restauracja", "Warszawa", "Łódź", "Gdańsk", "Kraków", "Wrocław", "Częstochowa", "Rzeszów", "Białystok",
            "Szczecin", "Bydgoszcz", "Gorzów Wielkopolski", "Zielona Góra",
        ).forEach { assertEquals(it, AnswerVerdict.CORRECT, verdict(it, it)) }
        assertEquals(AnswerVerdict.ALMOST, verdict("Łódź", "Lodz"))
        assertEquals(AnswerVerdict.WRONG, verdict("interesujący", "interesująncy"))
    }

    @Test
    fun `the keys of lesson 2 are accepted as written and in lower case`() {
        listOf(
            "się", "nazywa", "Imię", "Przepraszam", "Krakowie", "ulica", "powtórzyć", "USA", "przez", "jesteś", "Jesteśmy",
            "jesteście", "są", "panowie", "państwo", "mieszkają", "mieszkacie", "mają", "gracie", "rozumiemy",
            "co słychać", "wszystko w porządku", "dziękuję, wszystko świetnie", "jedenaście", "dziewiętnaście",
            "dwadzieścia dziewięć", "Dworzec", "do widzenia", "Gdzie wy jesteście?", "Dlaczego uczy się pani angielskiego?",
        ).forEach {
            assertEquals(it, AnswerVerdict.CORRECT, verdict(it, it))
            assertEquals(it, AnswerVerdict.CORRECT, verdict(it, it.lowercase()))
        }
        assertEquals(AnswerVerdict.ALMOST, verdict("dziękuję, wszystko świetnie", "dziekuje wszystko swietnie"))
        assertEquals(AnswerVerdict.ALMOST, verdict("jesteście", "jestescie"))
        assertEquals(AnswerVerdict.WRONG, verdict("jesteście", "jesteśce"))
    }

    @Test
    fun `a two-word gap accepts either order its key lists, and nothing else`() {
        val gap = listOf("się nazywasz", "nazywasz się")
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(gap, "się nazywasz"))
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(gap, "Nazywasz się"))
        assertEquals(AnswerVerdict.ALMOST, LessonAnswerChecker.verdict(gap, "sie nazywasz"))
        assertEquals(AnswerVerdict.WRONG, LessonAnswerChecker.verdict(gap, "nazywasz"))
        assertEquals(AnswerVerdict.WRONG, LessonAnswerChecker.verdict(gap, "się nazywacie"))
    }

    @Test
    fun `key words match whole words inside a longer answer`() {
        val reason = listOf("bo jej chłopak jest z Polski", "chłopak jest z Polski")
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(reason, "Bo mój chłopak jest z Polski!", AnswerMatch.KEY_WORDS))
        assertEquals(AnswerVerdict.WRONG, LessonAnswerChecker.verdict(reason, "chłopak", AnswerMatch.KEY_WORDS))
        assertEquals(AnswerVerdict.WRONG, LessonAnswerChecker.verdict(listOf("Rzym"), "Rzymie", AnswerMatch.KEY_WORDS))
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(listOf("12 422 20 12"), "tel. 12 422 20 12", AnswerMatch.KEY_WORDS))
        assertEquals(AnswerVerdict.ALMOST, LessonAnswerChecker.verdict(listOf("z Włoch", "Włoch"), "z Wloch", AnswerMatch.KEY_WORDS))
        assertEquals(AnswerVerdict.EMPTY, LessonAnswerChecker.verdict(listOf("Stein"), " ", AnswerMatch.KEY_WORDS))
    }

    @Test
    fun `sentence answers of lesson 3 ignore case and punctuation and take the optional second sentence`() {
        val chair = listOf("Nie, to nie jest klucz.", "Nie, to nie jest klucz. To jest krzesło.")
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(chair, "nie to nie jest klucz"))
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(chair, "Nie, to nie jest klucz, to jest krzesło!"))
        assertEquals(AnswerVerdict.WRONG, LessonAnswerChecker.verdict(chair, "Nie, to jest nie klucz."))
        assertEquals(AnswerVerdict.ALMOST, LessonAnswerChecker.verdict(listOf("Ten ołówek jest żółty."), "Ten olowek jest zolty"))
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(listOf("Mam pytanie: co to jest?"), "Mam pytanie - co to jest"))
        listOf("książka", "długopis", "krzesło", "stół", "ołówek", "płyta CD", "pomarańczowy", "brązowy", "żółty").forEach {
            assertEquals(it, AnswerVerdict.CORRECT, verdict(it, it))
        }
    }

    @Test
    fun `the answer shown back is the alternative closest to what was typed`() {
        val coffee = listOf("Kawa i cukier są tam.", "Cukier i kawa są tam.", "Tam są kawa i cukier.")
        assertEquals("Tam są kawa i cukier.", LessonAnswerChecker.closest(coffee, "Tam sa kawa i cukier"))
        assertEquals("Kawa i cukier są tam.", LessonAnswerChecker.closest(coffee, "kawa cukier"))
    }
}
