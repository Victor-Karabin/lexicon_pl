package com.lexicon.data.remote.translate

import com.lexicon.boundary.TranslationDirection
import com.lexicon.boundary.Translator
import com.lexicon.data.remote.httpGet
import com.lexicon.data.remote.urlEncoded
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val googleJson = Json { ignoreUnknownKeys = true }

private const val EN = "en"
private const val PL = "pl"

class IosGoogleTranslator(
    private val apiKey: String,
) : Translator {
    override suspend fun translate(
        text: String,
        direction: TranslationDirection,
    ): String? {
        if (apiKey.isBlank()) return null

        val (source, target) = when (direction) {
            TranslationDirection.EN_TO_PL -> EN to PL
            TranslationDirection.PL_TO_EN -> PL to EN
        }
        val body = httpGet(
            "https://translation.googleapis.com/language/translate/v2" +
                "?key=${apiKey.urlEncoded()}&q=${text.urlEncoded()}&source=$source&target=$target&format=text",
        ) ?: return null

        return runCatching {
            googleJson
                .decodeFromString<GoogleTranslateResponse>(body)
                .data
                ?.translations
                ?.firstOrNull()
                ?.translatedText
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}

@Serializable
private data class GoogleTranslateResponse(val data: GoogleTranslateData? = null)

@Serializable
private data class GoogleTranslateData(val translations: List<GoogleTranslation> = emptyList())

@Serializable
private data class GoogleTranslation(val translatedText: String = "")
