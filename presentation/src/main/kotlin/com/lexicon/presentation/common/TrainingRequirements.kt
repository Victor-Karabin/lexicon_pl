package com.lexicon.presentation.common

import com.lexicon.interactors.crossword.StartCrosswordSessionRequest
import com.lexicon.interactors.imagetest.StartImageTestSessionRequest
import com.lexicon.interactors.memorycards.StartMemoryCardsSessionRequest
import com.lexicon.presentation.main.TrainingIds

object TrainingRequirements {
    const val SINGLE_WORD_STEP = 1

    const val TRUE_OR_FALSE = 2

    const val IMAGE_TEST = StartImageTestSessionRequest.DEFAULT_OPTION_COUNT

    const val MEMORY_CARDS = StartMemoryCardsSessionRequest.DEFAULT_PAIRS_PER_STEP

    const val WORD_MATCH = 4

    const val CROSSWORD = StartCrosswordSessionRequest.DEFAULT_WORD_COUNT

    const val MIX = IMAGE_TEST

    fun minimumWordsFor(training: String): Int =
        when (training) {
            TrainingIds.TRUE_OR_FALSE -> TRUE_OR_FALSE
            TrainingIds.WORD_MATCH -> WORD_MATCH
            TrainingIds.IMAGE_TEST -> IMAGE_TEST
            TrainingIds.MEMORY_CARDS -> MEMORY_CARDS
            TrainingIds.CROSSWORD -> CROSSWORD
            TrainingIds.MIX -> MIX
            else -> SINGLE_WORD_STEP
        }
}
