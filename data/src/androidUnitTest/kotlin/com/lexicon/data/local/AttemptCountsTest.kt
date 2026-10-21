package com.lexicon.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class AttemptCountsTest {
    @Test
    fun `a first right answer starts every count`() {
        assertEquals(AttemptCounts(attempted = 1, correct = 1, incorrect = 0, streak = 1), AttemptCounts().after(isCorrect = true))
    }

    @Test
    fun `a first wrong answer counts as tried and incorrect with no streak`() {
        assertEquals(AttemptCounts(attempted = 1, correct = 0, incorrect = 1, streak = 0), AttemptCounts().after(isCorrect = false))
    }

    @Test
    fun `a right answer extends the streak`() {
        assertEquals(
            AttemptCounts(attempted = 4, correct = 3, incorrect = 1, streak = 3),
            AttemptCounts(attempted = 3, correct = 2, incorrect = 1, streak = 2).after(isCorrect = true),
        )
    }

    @Test
    fun `a wrong answer resets the streak`() {
        assertEquals(
            AttemptCounts(attempted = 4, correct = 2, incorrect = 2, streak = 0),
            AttemptCounts(attempted = 3, correct = 2, incorrect = 1, streak = 2).after(isCorrect = false),
        )
    }
}
