package com.lexicon.interactors.presets

interface GenerateWordExampleUseCase {
    suspend operator fun invoke(
        text: String,
        translation: String,
        level: String = "",
    ): String?
}
