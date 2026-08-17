package com.lexicon.interactors.presets

interface GetPinnedImageUseCase {
    suspend operator fun invoke(translation: String): String?
}
