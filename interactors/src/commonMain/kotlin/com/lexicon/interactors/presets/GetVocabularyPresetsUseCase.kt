package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.VocabularyPreset
import kotlinx.collections.immutable.ImmutableList

interface GetVocabularyPresetsUseCase {
    suspend operator fun invoke(): ImmutableList<VocabularyPreset>
}
