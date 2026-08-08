package com.lexicon.presentation.common

import com.lexicon.interactors.crossword.StartCrosswordSessionRequest
import com.lexicon.interactors.imagetest.StartImageTestSessionRequest
import com.lexicon.interactors.memorycards.StartMemoryCardsSessionRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the requirements to the parameters they come from. Restating a number here that the
 * training later changed is exactly the failure these guard against — the gate would pass and
 * the training would still be unrunnable.
 */
class TrainingRequirementsTest {
    @Test
    fun `Image Test needs enough words to fill every option`() {
        assertEquals(StartImageTestSessionRequest.DEFAULT_OPTION_COUNT, TrainingRequirements.IMAGE_TEST)
    }

    @Test
    fun `Memory Cards needs a full board of pairs`() {
        assertEquals(StartMemoryCardsSessionRequest.DEFAULT_PAIRS_PER_STEP, TrainingRequirements.MEMORY_CARDS)
    }

    @Test
    fun `Crossword needs as many words as the grid is built from`() {
        assertEquals(StartCrosswordSessionRequest.DEFAULT_WORD_COUNT, TrainingRequirements.CROSSWORD)
    }

    /** Mix delegates to its step types, so it cannot be easier to start than the hardest one. */
    @Test
    fun `Mix is at least as demanding as the step types it contains`() {
        assertTrue(TrainingRequirements.MIX >= TrainingRequirements.IMAGE_TEST)
        assertTrue(TrainingRequirements.MIX >= TrainingRequirements.TRUE_OR_FALSE)
        assertTrue(TrainingRequirements.MIX >= TrainingRequirements.SINGLE_WORD_STEP)
    }

    /** True or False needs a second word to draw a wrong translation from. */
    @Test
    fun `True or False needs more than one word`() {
        assertTrue(TrainingRequirements.TRUE_OR_FALSE > 1)
    }

    @Test
    fun `every requirement is a positive number of words`() {
        val requirements = listOf(
            TrainingRequirements.SINGLE_WORD_STEP,
            TrainingRequirements.TRUE_OR_FALSE,
            TrainingRequirements.IMAGE_TEST,
            TrainingRequirements.MEMORY_CARDS,
            TrainingRequirements.WORD_MATCH,
            TrainingRequirements.CROSSWORD,
            TrainingRequirements.MIX,
        )

        assertTrue(requirements.all { it >= 1 })
    }
}
