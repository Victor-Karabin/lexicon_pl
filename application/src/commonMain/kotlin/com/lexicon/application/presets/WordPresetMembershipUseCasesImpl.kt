package com.lexicon.application.presets

import com.lexicon.boundary.VocabularyPresetRepository
import com.lexicon.interactors.presets.GetWordPresetMembershipsUseCase
import com.lexicon.interactors.presets.PresetMembership
import com.lexicon.interactors.presets.SetWordPresetMembershipUseCase
import com.lexicon.model.vocabulary.PresetId
import com.lexicon.model.vocabulary.VocabularyId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class GetWordPresetMembershipsUseCaseImpl(
    private val repository: VocabularyPresetRepository,
) : GetWordPresetMembershipsUseCase {
    override suspend fun invoke(wordId: VocabularyId): ImmutableList<PresetMembership> {
        val memberOf = repository.getPresetIdsForWord(wordId.value).toSet()
        val categories = repository.getCategories().associate { it.id to it.toCategory() }
        return repository.getPresets()
            .mapNotNull { preset -> categories[preset.categoryId]?.let(preset::toPreset) }
            .sortedWith(compareBy({ it.category.order }, { it.popularity }))
            .map { PresetMembership(preset = it, isMember = it.id.value in memberOf) }
            .toImmutableList()
    }
}

class SetWordPresetMembershipUseCaseImpl(
    private val repository: VocabularyPresetRepository,
) : SetWordPresetMembershipUseCase {
    override suspend fun invoke(
        presetId: PresetId,
        wordId: VocabularyId,
        isMember: Boolean,
    ) = repository.setWordInPreset(presetId.value, wordId.value, isMember)
}
