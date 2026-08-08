package com.lexicon.interactors.presets

import kotlinx.collections.immutable.ImmutableList

/**
 * Finds words by either language and by CEFR level.
 *
 * Both narrowings are optional and combine: a query alone searches every level, levels alone
 * list every word at them, and together they do both. With neither, there is nothing to show
 * — that is the state where the presets belong on screen instead.
 */
interface SearchVocabularyUseCase {
    suspend operator fun invoke(
        query: String = "",
        levels: Set<CefrLevel> = emptySet(),
        limit: Int = DEFAULT_LIMIT,
    ): ImmutableList<PresetWord>

    companion object {
        /**
         * Above the size of the whole vocabulary, so selecting a level really does show every
         * word at it. The cap exists to stop an unbounded query as the corpus grows, not to
         * trim the answer.
         */
        const val DEFAULT_LIMIT = 5_000
    }
}
