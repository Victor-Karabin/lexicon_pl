package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word

interface UpdateWordUseCase {
    suspend operator fun invoke(
        id: VocabularyId,
        text: String,
        translation: String,
        imageUrl: String? = null,
        presetIds: List<PresetId> = emptyList(),
    ): Result<Word>
}
