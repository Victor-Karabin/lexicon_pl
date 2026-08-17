package com.lexicon.interactors.presets

interface GetVocabularyPresetUseCase {
    suspend operator fun invoke(id: PresetId): VocabularyPreset?
}
