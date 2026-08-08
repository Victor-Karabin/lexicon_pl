package com.lexicon.domain.presets

import com.lexicon.boundary.VocabularyPresetRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.presets.ObserveFavouriteWordIdsUseCase
import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.SetPresetFavouriteUseCase
import com.lexicon.interactors.presets.ToggleWordFavouriteUseCase
import com.lexicon.interactors.presets.VocabularyId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ToggleWordFavouriteUseCaseImpl
    @Inject
    constructor(
        private val vocabularyRepository: VocabularyRepository,
    ) : ToggleWordFavouriteUseCase {
        override suspend fun invoke(
            id: VocabularyId,
            isFavourite: Boolean,
        ) = vocabularyRepository.setFavourite(listOf(id.value), isFavourite)
    }

class SetPresetFavouriteUseCaseImpl
    @Inject
    constructor(
        private val presetRepository: VocabularyPresetRepository,
        private val vocabularyRepository: VocabularyRepository,
    ) : SetPresetFavouriteUseCase {
        override suspend fun invoke(
            id: PresetId,
            isFavourite: Boolean,
        ) {
            val preset = presetRepository.getPreset(id.value) ?: return
            vocabularyRepository.setFavourite(preset.vocabularyIds, isFavourite)
        }
    }

class ObserveFavouriteWordIdsUseCaseImpl
    @Inject
    constructor(
        private val vocabularyRepository: VocabularyRepository,
    ) : ObserveFavouriteWordIdsUseCase {
        override fun invoke(): Flow<Set<VocabularyId>> =
            vocabularyRepository.observeFavouriteIds().map { ids -> ids.mapTo(mutableSetOf(), ::VocabularyId) }
    }
