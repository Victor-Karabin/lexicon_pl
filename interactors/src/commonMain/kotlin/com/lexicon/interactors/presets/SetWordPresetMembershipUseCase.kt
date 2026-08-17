package com.lexicon.interactors.presets

interface SetWordPresetMembershipUseCase {
    suspend operator fun invoke(
        presetId: PresetId,
        wordId: VocabularyId,
        isMember: Boolean,
    )
}
