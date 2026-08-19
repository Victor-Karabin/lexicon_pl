package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.Word
import kotlinx.collections.immutable.ImmutableList

interface GetPresetVocabularyUseCase {
    suspend operator fun invoke(id: PresetId): ImmutableList<Word>
}
