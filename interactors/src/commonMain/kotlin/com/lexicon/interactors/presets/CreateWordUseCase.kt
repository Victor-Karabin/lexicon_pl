package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.PresetId
import com.lexicon.model.vocabulary.Word

interface CreateWordUseCase {
    suspend operator fun invoke(
        text: String,
        translation: String,
        imageUrl: String? = null,
        example: String = "",
        presetIds: List<PresetId> = emptyList(),
    ): Result<Word>
}
