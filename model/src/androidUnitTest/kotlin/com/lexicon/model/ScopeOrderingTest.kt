package com.lexicon.model.program

import com.lexicon.model.vocabulary.CefrLevel
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import org.junit.Assert.assertEquals
import org.junit.Test

class ScopeOrderingTest {
    private fun word(
        id: Long,
        text: String,
        cefr: CefrLevel? = null,
    ) = Word(VocabularyId(id), text, "meaning$id", "ipa$id", cefr = cefr)

    private val words = listOf(
        word(40, "zebra", CefrLevel.B1),
        word(10, "dom", CefrLevel.A1),
        word(30, "kot", CefrLevel.A2),
        word(20, "aleja", null),
    )

    private fun ids(ordered: List<Word>) = ordered.map { it.id.value }

    @Test
    fun `as listed keeps the order the sources produced`() {
        assertEquals(ids(words), ids(ScopeOrdering.AS_LISTED.applyTo(words)))
    }

    /** The shipped catalogue is numbered by how common a word is: Top 100 is ids 1..100. */
    @Test
    fun `frequency is the catalogue's own numbering`() {
        assertEquals(listOf(10L, 20L, 30L, 40L), ids(ScopeOrdering.FREQUENCY.applyTo(words)))
    }

    @Test
    fun `difficulty climbs the CEFR levels`() {
        assertEquals(listOf(10L, 30L, 40L, 20L), ids(ScopeOrdering.DIFFICULTY.applyTo(words)))
    }

    @Test
    fun `a word with no level is left until last, since nothing is known about it`() {
        assertEquals(20L, ScopeOrdering.DIFFICULTY.applyTo(words).last().id.value)
    }

    @Test
    fun `words of one level fall back to frequency inside it`() {
        val sameLevel = listOf(word(30, "kot", CefrLevel.A1), word(10, "dom", CefrLevel.A1))
        assertEquals(listOf(10L, 30L), ids(ScopeOrdering.DIFFICULTY.applyTo(sameLevel)))
    }

    @Test
    fun `alphabetical orders by the Polish word`() {
        assertEquals(listOf("aleja", "dom", "kot", "zebra"), ScopeOrdering.ALPHABETICAL.applyTo(words).map { it.text })
    }

    @Test
    fun `random keeps every word, however it shuffles them`() {
        assertEquals(ids(words).sorted(), ids(ScopeOrdering.RANDOM.applyTo(words)).sorted())
    }

    @Test
    fun `no ordering ever loses or invents a word`() {
        ScopeOrdering.entries.forEach { ordering ->
            assertEquals(ordering.name, ids(words).sorted(), ids(ordering.applyTo(words)).sorted())
        }
    }
}
