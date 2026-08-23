package com.lexicon.data.remote.translate

import com.lexicon.boundary.TranslationDirection
import com.lexicon.boundary.Translator

class GoogleTranslator(
    private val api: GoogleTranslateApi,
) : Translator {
    override suspend fun translate(
        text: String,
        direction: TranslationDirection,
    ): String? {
        val (source, target) = direction.languages()

        return runCatching {
            api
                .translate(text = text, source = source, target = target)
                .data
                ?.translations
                ?.firstOrNull()
                ?.translatedText
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}

internal fun TranslationDirection.languages(): Pair<String, String> =
    when (this) {
        TranslationDirection.EN_TO_PL -> ENGLISH to POLISH
        TranslationDirection.PL_TO_EN -> POLISH to ENGLISH
    }

internal const val ENGLISH = "en"
internal const val POLISH = "pl"
