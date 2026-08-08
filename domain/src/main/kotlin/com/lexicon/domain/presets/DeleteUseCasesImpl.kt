package com.lexicon.domain.presets

import com.lexicon.boundary.VocabularyPresetRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.presets.DeletePresetUseCase
import com.lexicon.interactors.presets.DeleteWordUseCase
import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.RestorePresetUseCase
import com.lexicon.interactors.presets.RestoreWordUseCase
import com.lexicon.interactors.presets.VocabularyId
import javax.inject.Inject

class DeleteWordUseCaseImpl
    @Inject
    constructor(
        private val vocabularyRepository: VocabularyRepository,
    ) : DeleteWordUseCase {
        override suspend fun invoke(id: VocabularyId) = vocabularyRepository.deleteWord(id.value)
    }

class RestoreWordUseCaseImpl
    @Inject
    constructor(
        private val vocabularyRepository: VocabularyRepository,
    ) : RestoreWordUseCase {
        override suspend fun invoke(id: VocabularyId) = vocabularyRepository.restoreWord(id.value)
    }

class DeletePresetUseCaseImpl
    @Inject
    constructor(
        private val presetRepository: VocabularyPresetRepository,
    ) : DeletePresetUseCase {
        override suspend fun invoke(id: PresetId) = presetRepository.deletePreset(id.value)
    }

class RestorePresetUseCaseImpl
    @Inject
    constructor(
        private val presetRepository: VocabularyPresetRepository,
    ) : RestorePresetUseCase {
        override suspend fun invoke(id: PresetId) = presetRepository.restorePreset(id.value)
    }
