package com.lexicon.interactors.presets

import kotlinx.collections.immutable.ImmutableList

interface GetPresetVocabularyUseCase {
    suspend operator fun invoke(id: PresetId): ImmutableList<PresetWord>
}
