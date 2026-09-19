package com.lexicon.interactors.presets

import kotlinx.collections.immutable.ImmutableList

interface SuggestTranslationsUseCase {
    suspend operator fun invoke(
        text: String,
        toPolish: Boolean,
    ): ImmutableList<String>

    companion object {
        const val VARIANTS = 4
    }
}
