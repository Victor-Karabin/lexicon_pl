package com.lexicon.application.presets

import com.lexicon.boundary.VocabularyPresetRepository
import com.lexicon.interactors.presets.SetWordPresetUseCase
import com.lexicon.model.vocabulary.PresetId
import com.lexicon.model.vocabulary.VocabularyId

class SetWordPresetUseCaseImpl(
    private val presets: VocabularyPresetRepository,
) : SetWordPresetUseCase {
    override suspend fun invoke(
        wordId: VocabularyId,
        presetId: PresetId,
        isMember: Boolean,
    ): Result<Unit> =
        runCatching {
            presets.setWordInPreset(presetId = presetId.value, wordId = wordId.value, isMember = isMember)
        }
}
