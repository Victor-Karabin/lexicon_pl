package com.lexicon.application.presets

import com.lexicon.boundary.TranslationDirection
import com.lexicon.boundary.TranslationSuggester
import com.lexicon.interactors.presets.SuggestTranslationsUseCase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

class SuggestTranslationsUseCaseImpl(
    private val suggester: TranslationSuggester,
) : SuggestTranslationsUseCase {
    override suspend fun invoke(
        text: String,
        toPolish: Boolean,
    ): ImmutableList<String> {
        val wanted = text.trim()
        if (wanted.isEmpty()) return persistentListOf()

        val direction = if (toPolish) TranslationDirection.EN_TO_PL else TranslationDirection.PL_TO_EN
        return suggester.suggest(wanted, direction, SuggestTranslationsUseCase.VARIANTS).toImmutableList()
    }
}
