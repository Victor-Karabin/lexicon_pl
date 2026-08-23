package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.PresetId
import com.lexicon.model.vocabulary.VocabularyId

interface SetWordPresetUseCase {
    suspend operator fun invoke(
        wordId: VocabularyId,
        presetId: PresetId,
        isMember: Boolean,
    ): Result<Unit>
}
