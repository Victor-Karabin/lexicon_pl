package com.lexicon.model.vocabulary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleSentenceTest {
    @Test
    fun `the markers come out and the word they wrapped is remembered`() {
        val sentence = ExampleSentence.parse("Mam czarnego **kota**.")

        assertEquals("Mam czarnego kota.", sentence.text)
        assertEquals(listOf(13 until 17), sentence.emphasis)
        assertEquals("kota", sentence.text.substring(13, 17))
    }

    @Test
    fun `a sentence can emphasise more than one word`() {
        val sentence = ExampleSentence.parse("**Idę** do domu i **czytam**.")

        assertEquals("Idę do domu i czytam.", sentence.text)
        assertEquals(listOf("Idę", "czytam"), sentence.emphasis.map { sentence.text.substring(it) })
    }

    @Test
    fun `an unmarked sentence still reads, with nothing emphasised`() {
        val sentence = ExampleSentence.parse("  Lubię kawę.  ")

        assertEquals("Lubię kawę.", sentence.text)
        assertTrue(sentence.emphasis.isEmpty())
    }

    @Test
    fun `a marker left open is text, not a range`() {
        val sentence = ExampleSentence.parse("Mam **kota")

        assertEquals("Mam **kota", sentence.text)
        assertTrue(sentence.emphasis.isEmpty())
    }

    @Test
    fun `empty markers emphasise nothing`() {
        val sentence = ExampleSentence.parse("Mam ****kota.")

        assertEquals("Mam kota.", sentence.text)
        assertTrue(sentence.emphasis.isEmpty())
    }

    @Test
    fun `nothing at all is blank`() {
        assertTrue(ExampleSentence.parse("   ").isBlank)
    }

    @Test
    fun `marking is what parsing undoes`() {
        val marked = "Mam czarnego **kota**."
        val sentence = ExampleSentence.parse(marked)

        assertEquals(marked, ExampleSentence.mark(sentence.text, sentence.emphasis))
    }

    @Test
    fun `an unmarked sentence takes its emphasis from the word itself`() {
        val sentence = ExampleSentence.of("Chcę kawę, ale bez cukru.", word = "ale")

        assertEquals("Chcę kawę, ale bez cukru.", sentence.text)
        assertEquals(listOf("ale"), sentence.emphasis.map { sentence.text.substring(it) })
    }

    @Test
    fun `an inflected form is found by what it still shares with the word`() {
        val sentence = ExampleSentence.of("Ta kobietę widziałem wczoraj.", word = "kobieta")

        assertEquals(listOf("kobietę"), sentence.emphasis.map { sentence.text.substring(it) })
    }

    @Test
    fun `a shortened stem still finds the word`() {
        val sentence = ExampleSentence.of("Na stole leży zamku klucz.", word = "zamek")

        assertEquals(listOf("zamku"), sentence.emphasis.map { sentence.text.substring(it) })
    }

    @Test
    fun `a two word entry lights up both of its words`() {
        val sentence = ExampleSentence.of("Muszę bać się psa.", word = "bać się")

        assertEquals(listOf("bać się"), sentence.emphasis.map { sentence.text.substring(it) })
    }

    @Test
    fun `markers already in the sentence win over matching`() {
        val sentence = ExampleSentence.of("**Idę** do domu.", word = "iść")

        assertEquals(listOf("Idę"), sentence.emphasis.map { sentence.text.substring(it) })
    }

    @Test
    fun `a stem that alternates away is left unemphasised rather than guessed at`() {
        val sentence = ExampleSentence.of("Idę do domu.", word = "iść")

        assertEquals("Idę do domu.", sentence.text)
        assertTrue(sentence.emphasis.isEmpty())
    }

    @Test
    fun `a word too short to match safely emphasises nothing`() {
        assertTrue(ExampleSentence.of("To nie jest moje.", word = "w").emphasis.isEmpty())
    }

    @Test
    fun `text being edited keeps the space typed after the last word`() {
        assertEquals("Ta kobieta ", ExampleSentence.editableText("Ta kobieta "))
    }

    @Test
    fun `text being edited shows the sentence without its markers`() {
        assertEquals("Ta kobieta jest miła.", ExampleSentence.editableText("Ta **kobieta** jest miła."))
    }
}
