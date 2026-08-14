package com.lexicon.data.repository

import com.lexicon.boundary.TranslationDirection
import com.lexicon.boundary.Translator

/**
 * Asks each translator in turn and takes the first answer, in the order the
 * platform's DI module supplies them — corpus first, so a word the app already
 * knows costs nothing and works offline, then whatever remote service is
 * configured.
 *
 * Same shape as [FallbackImageProviderImpl], and for the same reason: which
 * translators exist is a per-platform question, and today only Android has a
 * remote one.
 */
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
