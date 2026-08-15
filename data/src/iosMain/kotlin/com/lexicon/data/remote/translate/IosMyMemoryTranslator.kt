package com.lexicon.data.remote.translate

import com.lexicon.boundary.TranslationDirection
import com.lexicon.boundary.Translator
import com.lexicon.data.remote.httpGet
import com.lexicon.data.remote.urlEncoded
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

private const val EN = "en"
private const val PL = "pl"

/**
 * MyMemory, which needs no API key — so the field fills itself in on a plain
 * checkout with nothing to sign up for.
 *
 * [looksLikeATranslation] is shared with Android rather than copied: a memory that
 * answers with a stray segment has to be rejected the same way on both, or the same
 * word would fill in on one platform and not the other.
 */
class IosMyMemoryTranslator : Translator {
    override suspend fun translate(
        text: String,
        direction: TranslationDirection,
    ): String? {
        val langPair = when (direction) {
            TranslationDirection.EN_TO_PL -> "$EN|$PL"
            TranslationDirection.PL_TO_EN -> "$PL|$EN"
        }
        val body = httpGet(
            "https://api.mymemory.translated.net/get?q=${text.urlEncoded()}&langpair=${langPair.urlEncoded()}",
        ) ?: return null

        return runCatching {
            json
                .decodeFromString<MyMemoryResponse>(body)
                .responseData
                ?.translatedText
                ?.trim()
                ?.takeIf { looksLikeATranslation(source = text, candidate = it) }
        }.getOrNull()
    }
}

@Serializable
private data class MyMemoryResponse(val responseData: MyMemoryData? = null)

@Serializable
private data class MyMemoryData(val translatedText: String? = null)
