package com.lexicon.interactors.presets

import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

/** Every preset, ordered by category and then by the catalogue's own order. */
interface GetVocabularyPresetsUseCase {
    suspend operator fun invoke(): ImmutableList<VocabularyPreset>
}

/**
 * The same list, re-emitted whenever it changes.
 *
 * A preset carries the word ids its heart and word count are derived from, so a screen holding
 * a list it fetched once shows the wrong ones the moment a word is deleted somewhere else.
 */
interface ObserveVocabularyPresetsUseCase {
    operator fun invoke(): Flow<ImmutableList<VocabularyPreset>>
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
