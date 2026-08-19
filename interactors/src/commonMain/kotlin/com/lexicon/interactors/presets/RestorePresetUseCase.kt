package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.PresetId

interface RestorePresetUseCase {
    suspend operator fun invoke(id: PresetId)
}
