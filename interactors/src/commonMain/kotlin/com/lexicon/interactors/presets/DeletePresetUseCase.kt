package com.lexicon.interactors.presets

interface DeletePresetUseCase {
    suspend operator fun invoke(id: PresetId)
}
