package com.lexicon.interactors.presets

import kotlinx.collections.immutable.ImmutableList

interface GetVocabularyPresetsUseCase {
    suspend operator fun invoke(): ImmutableList<VocabularyPreset>
}
