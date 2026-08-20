package com.lexicon.application.presets

import com.lexicon.boundary.VocabularyPresetRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.presets.DeletePresetUseCase
import com.lexicon.interactors.presets.DeleteWordUseCase
import com.lexicon.interactors.presets.RestorePresetUseCase
import com.lexicon.interactors.presets.RestoreWordUseCase
import com.lexicon.model.vocabulary.PresetId
import com.lexicon.model.vocabulary.VocabularyId

class DeleteWordUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
) : DeleteWordUseCase {
    override suspend fun invoke(id: VocabularyId) = vocabularyRepository.deleteWord(id.value)
}

class RestoreWordUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
) : RestoreWordUseCase {
    override suspend fun invoke(id: VocabularyId) = vocabularyRepository.restoreWord(id.value)
}

class DeletePresetUseCaseImpl(
    private val presetRepository: VocabularyPresetRepository,
) : DeletePresetUseCase {
    override suspend fun invoke(id: PresetId) = presetRepository.deletePreset(id.value)
}

class RestorePresetUseCaseImpl(
    private val presetRepository: VocabularyPresetRepository,
) : RestorePresetUseCase {
    override suspend fun invoke(id: PresetId) = presetRepository.restorePreset(id.value)
}
