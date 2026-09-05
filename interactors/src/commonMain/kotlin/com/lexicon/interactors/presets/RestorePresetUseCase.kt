package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.PresetId

fun interface RestorePresetUseCase {
    suspend operator fun invoke(id: PresetId)
}
