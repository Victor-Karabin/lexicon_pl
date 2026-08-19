package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.PresetId

interface SetPresetInStudySetUseCase {
    suspend operator fun invoke(
        id: PresetId,
        isInStudySet: Boolean,
    )
}
