package com.lexicon.interactors.presets

import kotlinx.collections.immutable.ImmutableList

interface SearchVocabularyUseCase {
    suspend operator fun invoke(
        query: String = "",
        levels: Set<CefrLevel> = emptySet(),
        limit: Int = DEFAULT_LIMIT,
    ): ImmutableList<PresetWord>

    companion object {
        const val DEFAULT_LIMIT = 5_000
    }
}
