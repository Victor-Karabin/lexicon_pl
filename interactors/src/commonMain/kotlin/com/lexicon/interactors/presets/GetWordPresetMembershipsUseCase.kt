package com.lexicon.interactors.presets

import kotlinx.collections.immutable.ImmutableList

interface GetWordPresetMembershipsUseCase {
    suspend operator fun invoke(wordId: VocabularyId): ImmutableList<PresetMembership>
}
