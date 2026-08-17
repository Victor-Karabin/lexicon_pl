package com.lexicon.interactors.presets

import kotlinx.collections.immutable.ImmutableList

interface SearchImageCandidatesUseCase {
    suspend operator fun invoke(
        query: String,
        skip: Int = 0,
    ): ImmutableList<String>
}
