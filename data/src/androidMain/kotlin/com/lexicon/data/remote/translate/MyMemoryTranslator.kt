package com.lexicon.data.remote.translate

import com.lexicon.boundary.TranslationDirection
import com.lexicon.boundary.Translator

private const val EN = "en"
private const val PL = "pl"

/**
 * Translates through MyMemory, which needs no API key — so the field fills itself
 * in on a plain checkout, with no signing up for anything.
 *
 * The catch is that MyMemory is a translation memory rather than an engine: it
 * answers with the closest segment somebody has already translated, and when it has
 * no good match it returns something that is not a translation at all. Asked for
 * *smok* it offered "Iain Walker wrote:". [looksLikeATranslation] is what keeps
 * answers like that out of the field.
 */
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
