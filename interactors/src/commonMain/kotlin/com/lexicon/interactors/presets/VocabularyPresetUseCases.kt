package com.lexicon.interactors.presets

import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

interface GetVocabularyPresetsUseCase {
    suspend operator fun invoke(): ImmutableList<VocabularyPreset>
}

interface ObserveVocabularyPresetsUseCase {
    operator fun invoke(): Flow<ImmutableList<VocabularyPreset>>
}

interface GetPresetCategoriesUseCase {
    suspend operator fun invoke(): ImmutableList<PresetCategory>
}

interface GetVocabularyPresetUseCase {
    suspend operator fun invoke(id: PresetId): VocabularyPreset?
}

interface GetPresetVocabularyUseCase {
    suspend operator fun invoke(id: PresetId): ImmutableList<PresetWord>
}
