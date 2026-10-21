package com.lexicon.application.training

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class NextToPractiseTest {
    private fun List<Int>.pick(
        asked: Set<Int> = emptySet(),
        mastered: Set<Int> = emptySet(),
        attempts: Map<Int, Int> = emptyMap(),
        random: Random = Random(1),
    ): Int? =
        nextToPractise(
            wasAsked = { it in asked },
            isMastered = { it in mastered },
            attempts = { attempts[it] ?: 0 },
            random = random,
        )

    @Test
    fun `nothing is picked from an empty list`() {
        assertNull(emptyList<Int>().pick())
    }

    @Test
    fun `items already asked are skipped`() {
        assertEquals(3, listOf(1, 2, 3).pick(asked = setOf(1, 2)))
    }

    @Test
    fun `when all were asked the list starts over`() {
        assertTrue(listOf(1, 2).pick(asked = setOf(1, 2)) in listOf(1, 2))
    }

    @Test
    fun `mastered items are skipped even when they were tried fewer times`() {
        assertEquals(2, listOf(1, 2).pick(mastered = setOf(1), attempts = mapOf(1 to 1, 2 to 9)))
    }

    @Test
    fun `when all are mastered the list is used whole`() {
        assertEquals(2, listOf(1, 2).pick(mastered = setOf(1, 2), attempts = mapOf(1 to 4, 2 to 1)))
    }

    @Test
    fun `asked items are dropped before mastered ones`() {
        assertEquals(3, listOf(1, 2, 3).pick(asked = setOf(1), mastered = setOf(2, 3), attempts = mapOf(2 to 5, 3 to 1)))
    }

    @Test
    fun `when everything left is mastered the asked ones do not come back`() {
        assertEquals(3, listOf(1, 2, 3).pick(asked = setOf(1), mastered = setOf(2, 3), attempts = mapOf(1 to 0, 2 to 5, 3 to 1)))
    }

    @Test
    fun `the item tried the fewest times wins`() {
        assertEquals(2, listOf(1, 2, 3).pick(attempts = mapOf(1 to 3, 2 to 1, 3 to 2)))
    }

    @Test
    fun `ties are broken at random`() {
        val picked = (0..40).map { listOf(1, 2, 3).pick(attempts = mapOf(3 to 2), random = Random(it)) }.toSet()
        assertEquals(setOf(1, 2), picked)
    }
}
