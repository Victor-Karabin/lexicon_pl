package com.lexicon.domain.presets

import com.lexicon.boundary.VocabularyPresetRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.presets.ObserveStudySetIdsUseCase
import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.SetPresetInStudySetUseCase
import com.lexicon.interactors.presets.ToggleWordInStudySetUseCase
import com.lexicon.model.vocabulary.VocabularyId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ToggleWordInStudySetUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
) : ToggleWordInStudySetUseCase {
    override suspend fun invoke(
        id: VocabularyId,
        isInStudySet: Boolean,
    ) = vocabularyRepository.setInStudySet(listOf(id.value), isInStudySet)
}

class SetPresetInStudySetUseCaseImpl(
    private val presetRepository: VocabularyPresetRepository,
    private val vocabularyRepository: VocabularyRepository,
) : SetPresetInStudySetUseCase {
    override suspend fun invoke(
        id: PresetId,
        isInStudySet: Boolean,
    ) {
        val preset = presetRepository.getPreset(id.value) ?: return
        vocabularyRepository.setInStudySet(preset.vocabularyIds, isInStudySet)
    }
}

class ObserveStudySetIdsUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
) : ObserveStudySetIdsUseCase {
    override fun invoke(): Flow<Set<VocabularyId>> =
        vocabularyRepository.observeStudySetIds().map { ids -> ids.mapTo(mutableSetOf(), ::VocabularyId) }
}
