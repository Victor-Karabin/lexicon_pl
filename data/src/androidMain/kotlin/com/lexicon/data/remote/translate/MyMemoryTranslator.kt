package com.lexicon.data.remote.translate

import com.lexicon.boundary.TranslationDirection
import com.lexicon.boundary.Translator

private const val EN = "en"
private const val PL = "pl"

class MyMemoryTranslator(
    private val api: MyMemoryApi,
) : Translator {
    override suspend fun translate(
        text: String,
        direction: TranslationDirection,
    ): String? {
        val langPair = when (direction) {
            TranslationDirection.EN_TO_PL -> "$EN|$PL"
            TranslationDirection.PL_TO_EN -> "$PL|$EN"
        }
        return runCatching {
            api
                .translate(text = text, langPair = langPair)
                .data
                ?.translatedText
                ?.trim()
                ?.takeIf { looksLikeATranslation(source = text, candidate = it) }
        }.getOrNull()
    }
}
