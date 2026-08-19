package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.PresetId

interface DeletePresetUseCase {
    suspend operator fun invoke(id: PresetId)
}
