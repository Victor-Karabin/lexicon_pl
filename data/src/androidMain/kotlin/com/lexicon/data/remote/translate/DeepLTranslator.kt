package com.lexicon.data.remote.translate

import com.lexicon.boundary.TranslationDirection
import com.lexicon.boundary.Translator

private const val EN = "EN"
private const val PL = "PL"

/**
 * Translates through DeepL, which handles Polish inflection better than the
 * general-purpose engines.
 *
 * A failure is an absent answer rather than an error: the caller is filling in a
 * field the learner can type over, so no network, a rate limit or a rejected key
 * should leave the field alone, not interrupt them. Whether this translator is in
 * the chain at all is decided at wiring time, by whether a key is configured.
 */
class DeepLTranslator(
    private val api: DeepLApi,
) : Translator {
    override suspend fun translate(
        text: String,
        direction: TranslationDirection,
    ): String? {
        val (source, target) = when (direction) {
            TranslationDirection.EN_TO_PL -> EN to PL
            TranslationDirection.PL_TO_EN -> PL to EN
        }
        return runCatching {
            api.translate(text = text, sourceLang = source, targetLang = target)
                .translations
                .firstOrNull()
                ?.text
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
