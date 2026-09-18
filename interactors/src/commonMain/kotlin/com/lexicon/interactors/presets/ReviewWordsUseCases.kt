package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.Word
import kotlinx.collections.immutable.ImmutableList

interface GetWordsToReviewUseCase {
    suspend operator fun invoke(limit: Int = BATCH): ImmutableList<Word>

    companion object {
        const val BATCH = 20
    }
}

interface CountWordsToReviewUseCase {
    suspend operator fun invoke(): Int
}
