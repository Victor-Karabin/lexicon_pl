package com.lexicon.boundary

enum class TranslationDirection { EN_TO_PL, PL_TO_EN }

interface Translator {
    suspend fun translate(
        text: String,
        direction: TranslationDirection,
    ): String?
}
