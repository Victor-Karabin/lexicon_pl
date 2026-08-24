package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.CefrLevel
import com.lexicon.model.vocabulary.Word
import kotlinx.collections.immutable.ImmutableList

interface SearchVocabularyUseCase {
    suspend operator fun invoke(
        query: String = "",
        levels: Set<CefrLevel> = emptySet(),
        limit: Int = PAGE,
        skip: Int = 0,
    ): ImmutableList<Word>

    companion object {
        const val PAGE = 40
    }
}
