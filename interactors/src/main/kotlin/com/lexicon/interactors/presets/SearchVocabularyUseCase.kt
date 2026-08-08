package com.lexicon.interactors.presets

import kotlinx.collections.immutable.ImmutableList

/**
 * Finds words by either language: typing "apple" or "jabłko" — or "jablko" — finds the same
 * entry. Which side matched is not reported, because the result shows both anyway.
 */
interface SearchVocabularyUseCase {
    suspend operator fun invoke(
        query: String,
        limit: Int = DEFAULT_LIMIT,
    ): ImmutableList<PresetWord>

    companion object {
        /** A two-letter query matches most of the vocabulary; the list is a lookup, not a dump. */
        const val DEFAULT_LIMIT = 100
    }
}
