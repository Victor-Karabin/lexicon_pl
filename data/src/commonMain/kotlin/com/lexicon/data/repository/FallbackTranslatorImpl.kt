package com.lexicon.data.repository

import com.lexicon.boundary.TranslationDirection
import com.lexicon.boundary.Translator

class FallbackTranslatorImpl(
    private val translators: List<Translator>,
) : Translator {
    override suspend fun translate(
        text: String,
        direction: TranslationDirection,
    ): String? {
        if (text.isBlank()) return null
        for (translator in translators) {
            val translated = translator.translate(text, direction)
            if (!translated.isNullOrBlank()) return translated
        }
        return null
    }
}
