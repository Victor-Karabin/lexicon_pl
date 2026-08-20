package com.lexicon.interactors.training

import com.lexicon.interactors.crossword.StartCrosswordSessionRequest
import com.lexicon.interactors.imagetest.StartImageTestSessionRequest
import com.lexicon.interactors.memorycards.StartMemoryCardsSessionRequest
import com.lexicon.model.training.TrainingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingTypeRequirementsTest {
    @Test
    fun `Image Test needs enough words to fill every option`() {
        assertEquals(StartImageTestSessionRequest.DEFAULT_OPTION_COUNT, TrainingType.IMAGE_TEST.minimumWords)
    }

    @Test
    fun `Memory Cards needs a full board of pairs`() {
        assertEquals(StartMemoryCardsSessionRequest.DEFAULT_PAIRS_PER_STEP, TrainingType.MEMORY_CARDS.minimumWords)
    }

    @Test
    fun `Crossword needs as many words as the grid is built from`() {
        assertEquals(StartCrosswordSessionRequest.DEFAULT_WORD_COUNT, TrainingType.CROSSWORD.minimumWords)
    }

    @Test
    fun `Mix is at least as demanding as the step types it contains`() {
        assertTrue(TrainingType.MIX.minimumWords >= TrainingType.IMAGE_TEST.minimumWords)
        assertTrue(TrainingType.MIX.minimumWords >= TrainingType.TRUE_OR_FALSE.minimumWords)
        assertTrue(TrainingType.MIX.minimumWords >= TrainingType.DICTATION.minimumWords)
    }

    @Test
    fun `True or False needs more than one word`() {
        assertTrue(TrainingType.TRUE_OR_FALSE.minimumWords > 1)
    }

    @Test
    fun `every training needs at least one word`() {
        assertTrue(TrainingType.entries.all { it.minimumWords >= 1 })
    }

    @Test
    fun `every training has a distinct id`() {
        assertEquals(TrainingType.entries.size, TrainingType.entries.map { it.id }.distinct().size)
    }

    @Test
    fun `every id maps back to its training`() {
        assertTrue(TrainingType.entries.all { TrainingType.ofId(it.id) == it })
    }
}
