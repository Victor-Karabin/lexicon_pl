package com.lexicon.domain.presets

import com.lexicon.boundary.VocabularyPresetRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.presets.GetPresetCategoriesUseCase
import com.lexicon.interactors.presets.GetPresetVocabularyUseCase
import com.lexicon.interactors.presets.GetVocabularyPresetUseCase
import com.lexicon.interactors.presets.GetVocabularyPresetsUseCase
import com.lexicon.interactors.presets.PresetCategory
import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.PresetWord
import com.lexicon.interactors.presets.VocabularyPreset
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

/**
 * Joins presets to their categories. A preset naming a category the catalogue does not
 * define is dropped rather than shown uncategorised — the validator reports it as a data
 * error, and a half-placed preset in the browser would only hide that.
 */
class GetVocabularyPresetsUseCaseImpl
    @Inject
    constructor(
        private val repository: VocabularyPresetRepository,
    ) : GetVocabularyPresetsUseCase {
        override suspend fun invoke(): ImmutableList<VocabularyPreset> {
            val categories = repository.getCategories().associate { it.id to it.toCategory() }
            return repository.getPresets()
                .mapNotNull { preset -> categories[preset.categoryId]?.let(preset::toPreset) }
                .sortedWith(compareBy({ it.category.order }, { it.popularity }))
                .toImmutableList()
        }
    }

class GetPresetCategoriesUseCaseImpl
    @Inject
    constructor(
        private val repository: VocabularyPresetRepository,
    ) : GetPresetCategoriesUseCase {
        override suspend fun invoke(): ImmutableList<PresetCategory> =
            repository.getCategories().map { it.toCategory() }.sortedBy { it.order }.toImmutableList()
    }

class GetVocabularyPresetUseCaseImpl
    @Inject
    constructor(
        private val repository: VocabularyPresetRepository,
    ) : GetVocabularyPresetUseCase {
        override suspend fun invoke(id: PresetId): VocabularyPreset? {
            val preset = repository.getPreset(id.value) ?: return null
            val category = repository.getCategories().firstOrNull { it.id == preset.categoryId } ?: return null
            return preset.toPreset(category.toCategory())
        }
    }

/**
 * Resolves a preset's ids into words. The order follows the preset rather than the store,
 * so a "100 most common words" preset is delivered in frequency order.
 */
class GetPresetVocabularyUseCaseImpl
    @Inject
    constructor(
        private val presetRepository: VocabularyPresetRepository,
        private val vocabularyRepository: VocabularyRepository,
    ) : GetPresetVocabularyUseCase {
        override suspend fun invoke(id: PresetId): ImmutableList<PresetWord> {
            val preset = presetRepository.getPreset(id.value) ?: return persistentListOf()
            val byId = vocabularyRepository.getItemsByIds(preset.vocabularyIds).associateBy { it.id }
            return preset.vocabularyIds.mapNotNull { byId[it]?.toPresetWord() }.toImmutableList()
        }
    }
