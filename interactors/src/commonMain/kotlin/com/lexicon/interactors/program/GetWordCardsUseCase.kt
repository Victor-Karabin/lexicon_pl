package com.lexicon.interactors.program

import com.lexicon.interactors.presets.VocabularyId
import kotlinx.collections.immutable.ImmutableList

/** The cards for today's new words, with whatever picture each already has. */
interface GetWordCardsUseCase {
    suspend operator fun invoke(ids: List<VocabularyId>): ImmutableList<WordCard>
}
