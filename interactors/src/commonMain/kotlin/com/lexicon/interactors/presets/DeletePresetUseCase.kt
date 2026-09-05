package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.PresetId

fun interface DeletePresetUseCase {
    suspend operator fun invoke(id: PresetId)
}
