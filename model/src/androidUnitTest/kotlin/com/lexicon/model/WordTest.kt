package com.lexicon.model.vocabulary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordTest {
    private val woda = Word(VocabularyId(1), "woda", "water", "ˈvɔda")

    @Test
    fun `a level the app does not know degrades to null rather than failing`() {
        assertNull(CefrLevel.ofName("D3"))
        assertNull(CefrLevel.ofName(null))
        assertEquals(CefrLevel.A1, CefrLevel.ofName("A1"))
    }

    @Test
    fun `a word will not exist without text`() {
        val failure = runCatching { woda.copy(text = " ") }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `starring a word already in the study set changes nothing`() {
        val starred = woda.addToStudySet()
        assertTrue(starred.isInStudySet)
        assertTrue(starred === starred.addToStudySet())
    }

    @Test
    fun `a word can leave the study set`() {
        assertFalse(woda.addToStudySet().removeFromStudySet().isInStudySet)
    }

    @Test
    fun `editing a word keeps its identity`() {
        val edited = woda.edited(translation = "the water")
        assertEquals(woda.id, edited.id)
        assertEquals("the water", edited.translation)
        assertEquals(woda.text, edited.text)
    }

    @Test
    fun `a phrase is a word with a space in it`() {
        assertFalse(woda.isPhrase)
        assertTrue(woda.copy(text = "dzień dobry").isPhrase)
    }
}
