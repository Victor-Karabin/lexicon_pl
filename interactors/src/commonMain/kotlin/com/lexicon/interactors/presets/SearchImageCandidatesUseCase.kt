package com.lexicon.interactors.presets

import kotlinx.collections.immutable.ImmutableList

/**
 * Pictures to choose between for a word. [skip] drops the ones already offered, so
 * asking for more turns up something new rather than the same batch again.
 */
interface SearchImageCandidatesUseCase {
    suspend operator fun invoke(
        query: String,
        skip: Int = 0,
    ): ImmutableList<String>
}
