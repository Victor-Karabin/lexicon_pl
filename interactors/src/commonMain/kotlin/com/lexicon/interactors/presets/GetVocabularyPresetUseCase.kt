package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.PresetId
import com.lexicon.model.vocabulary.VocabularyPreset

interface GetVocabularyPresetUseCase {
    suspend operator fun invoke(id: PresetId): VocabularyPreset?
}
