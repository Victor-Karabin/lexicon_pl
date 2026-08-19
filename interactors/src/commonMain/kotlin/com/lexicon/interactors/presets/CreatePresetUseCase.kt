package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.VocabularyPreset

interface CreatePresetUseCase {
    suspend operator fun invoke(
        title: String,
        description: String = "",
        icon: String? = null,
        color: String? = null,
        wordIds: List<VocabularyId> = emptyList(),
    ): Result<VocabularyPreset>
}
