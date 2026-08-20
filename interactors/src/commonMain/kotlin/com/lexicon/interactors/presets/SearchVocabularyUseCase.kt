package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.CefrLevel
import com.lexicon.model.vocabulary.Word
import kotlinx.collections.immutable.ImmutableList

interface SearchVocabularyUseCase {
    suspend operator fun invoke(
        query: String = "",
        levels: Set<CefrLevel> = emptySet(),
        limit: Int = DEFAULT_LIMIT,
    ): ImmutableList<Word>

    companion object {
        const val DEFAULT_LIMIT = 5_000
    }
}
