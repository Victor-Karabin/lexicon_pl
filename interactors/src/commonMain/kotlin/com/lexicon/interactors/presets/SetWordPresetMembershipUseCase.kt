package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.VocabularyId

interface SetWordPresetMembershipUseCase {
    suspend operator fun invoke(
        presetId: PresetId,
        wordId: VocabularyId,
        isMember: Boolean,
    )
}
