package com.lexicon.presentation.common

import com.lexicon.interactors.crossword.StartCrosswordSessionRequest
import com.lexicon.interactors.imagetest.StartImageTestSessionRequest
import com.lexicon.interactors.memorycards.StartMemoryCardsSessionRequest

/**
 * The smallest study set each training can still run on.
 *
 * These are what a training *cannot function* below, not what makes it pleasant. A Dictation
 * of three words is a short session; an Image Test of three words is a different, easier
 * exercise wearing Image Test's name, and that is the line these numbers draw.
 *
 * Derived from each training's own parameters rather than restated, so changing an option
 * count in one place cannot leave the requirement behind.
 */
object TrainingRequirements {
    /** One word is one step. Fewer steps than configured is a short session, not a broken one. */
    const val SINGLE_WORD_STEP = 1

    /** Needs a second word to draw a wrong translation from, or every answer is "true". */
    const val TRUE_OR_FALSE = 2

    /** Every option must be fillable, or the answer is obvious by elimination. */
    const val IMAGE_TEST = StartImageTestSessionRequest.DEFAULT_OPTION_COUNT

    /** A board is pairs of cards; below a full board there is nothing to remember. */
    const val MEMORY_CARDS = StartMemoryCardsSessionRequest.DEFAULT_PAIRS_PER_STEP

    /** Word Match is one board of pairs, and needs at least enough to be worth matching. */
    const val WORD_MATCH = 4

    /** A grid needs enough words to intersect; a two-word crossword is two words in a line. */
    const val CROSSWORD = StartCrosswordSessionRequest.DEFAULT_WORD_COUNT

    /** Mix includes Image Test, so it inherits the largest requirement of its step types. */
    const val MIX = IMAGE_TEST
}
