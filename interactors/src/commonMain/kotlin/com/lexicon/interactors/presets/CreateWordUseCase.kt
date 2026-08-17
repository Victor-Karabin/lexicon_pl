package com.lexicon.interactors.presets

interface CreateWordUseCase {
    suspend operator fun invoke(
        text: String,
        translation: String,
        imageUrl: String? = null,
        presetIds: List<PresetId> = emptyList(),
    ): Result<PresetWord>
}
