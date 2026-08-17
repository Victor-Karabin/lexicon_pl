package com.lexicon.interactors.presets

import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

interface ObserveVocabularyPresetsUseCase {
    operator fun invoke(): Flow<ImmutableList<VocabularyPreset>>
}
