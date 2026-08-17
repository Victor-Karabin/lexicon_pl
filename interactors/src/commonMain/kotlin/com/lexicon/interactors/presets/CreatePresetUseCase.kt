package com.lexicon.interactors.presets

interface CreatePresetUseCase {
    suspend operator fun invoke(
        title: String,
        description: String = "",
        icon: String? = null,
        color: String? = null,
        wordIds: List<VocabularyId> = emptyList(),
    ): Result<VocabularyPreset>
}
