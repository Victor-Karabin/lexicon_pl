package com.lexicon.interactors.presets

interface TranslateWordUseCase {
    suspend operator fun invoke(
        text: String,
        toPolish: Boolean,
    ): String?
}
