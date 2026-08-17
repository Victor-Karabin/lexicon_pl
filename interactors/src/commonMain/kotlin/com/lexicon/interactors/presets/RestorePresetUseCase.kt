package com.lexicon.interactors.presets

interface RestorePresetUseCase {
    suspend operator fun invoke(id: PresetId)
}
