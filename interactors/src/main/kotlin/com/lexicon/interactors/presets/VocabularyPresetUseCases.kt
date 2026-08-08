package com.lexicon.interactors.presets

import kotlinx.collections.immutable.ImmutableList

/** Every preset, ordered by category and then by the catalogue's own order. */
interface GetVocabularyPresetsUseCase {
    suspend operator fun invoke(): ImmutableList<VocabularyPreset>
}

interface GetPresetCategoriesUseCase {
    suspend operator fun invoke(): ImmutableList<PresetCategory>
}

/** Null when nothing carries the id, which happens if saved state outlives a preset. */
interface GetVocabularyPresetUseCase {
    suspend operator fun invoke(id: PresetId): VocabularyPreset?
}

/** The words a preset contains, for the trainings that are about to use them. */
interface GetPresetVocabularyUseCase {
    suspend operator fun invoke(id: PresetId): ImmutableList<PresetWord>
}
