package com.lexicon.model.program

import org.junit.Assert.assertEquals
import org.junit.Test

class ScopeOrderingTest {
    private val ids = listOf(5L, 1L, 4L, 2L, 3L)

    @Test
    fun `as listed keeps the order the sources produced`() {
        assertEquals(ids, ScopeOrdering.AS_LISTED.applyTo(ids))
    }

    @Test
    fun `difficulty orders by id`() {
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), ScopeOrdering.DIFFICULTY.applyTo(ids))
    }

    @Test
    fun `random keeps every word, however it shuffles them`() {
        assertEquals(ids.sorted(), ScopeOrdering.RANDOM.applyTo(ids).sorted())
    }

    /**
     * Kept as found rather than guessed at: these two order nothing today, and the
     * project does not say whether that is unfinished or deliberate. The test records
     * the behaviour so a change to it is a decision rather than an accident.
     */
    @Test
    fun `frequency and alphabetical do not order anything yet`() {
        assertEquals(ids, ScopeOrdering.FREQUENCY.applyTo(ids))
        assertEquals(ids, ScopeOrdering.ALPHABETICAL.applyTo(ids))
    }

    @Test
    fun `no ordering ever loses or invents a word`() {
        ScopeOrdering.entries.forEach { ordering ->
            assertEquals(ordering.name, ids.sorted(), ordering.applyTo(ids).sorted())
        }
    }
}
