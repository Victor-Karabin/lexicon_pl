package com.lexicon.presentation.common

import com.lexicon.interactors.crossword.StartCrosswordSessionRequest
import com.lexicon.interactors.imagetest.StartImageTestSessionRequest
import com.lexicon.interactors.memorycards.StartMemoryCardsSessionRequest

object TrainingRequirements {
    const val SINGLE_WORD_STEP = 1

    const val TRUE_OR_FALSE = 2

    const val IMAGE_TEST = StartImageTestSessionRequest.DEFAULT_OPTION_COUNT

    const val MEMORY_CARDS = StartMemoryCardsSessionRequest.DEFAULT_PAIRS_PER_STEP

    const val WORD_MATCH = 4

    const val CROSSWORD = StartCrosswordSessionRequest.DEFAULT_WORD_COUNT

    const val MIX = IMAGE_TEST
}
