package com.lexicon.interactors.presets

interface SetPresetInStudySetUseCase {
    suspend operator fun invoke(
        id: PresetId,
        isInStudySet: Boolean,
    )
}
