package com.lexicon.data.remote.translate

import com.lexicon.boundary.TranslationDirection
import com.lexicon.boundary.Translator

private const val EN = "EN"
private const val PL = "PL"

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
