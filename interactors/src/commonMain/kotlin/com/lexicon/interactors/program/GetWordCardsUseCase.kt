package com.lexicon.interactors.program

import com.lexicon.interactors.presets.VocabularyId
import kotlinx.collections.immutable.ImmutableList

interface GetWordCardsUseCase {
    suspend operator fun invoke(ids: List<VocabularyId>): ImmutableList<WordCard>
}
