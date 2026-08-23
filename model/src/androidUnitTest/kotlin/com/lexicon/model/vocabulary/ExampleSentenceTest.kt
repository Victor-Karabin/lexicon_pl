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
}
