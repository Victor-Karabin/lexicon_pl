package com.lexicon.interactors.presets

interface UpdateWordUseCase {
    suspend operator fun invoke(
        id: VocabularyId,
        text: String,
        translation: String,
        imageUrl: String? = null,
        presetIds: List<PresetId> = emptyList(),
    ): Result<PresetWord>
}
