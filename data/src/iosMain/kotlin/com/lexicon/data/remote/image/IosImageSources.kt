package com.lexicon.data.remote.image

import com.lexicon.data.remote.httpGet
import com.lexicon.data.remote.urlEncoded
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

class OpenverseIosImageSource : RemoteImageSource {
    override suspend fun searchImageUrls(
        query: String,
        count: Int,
    ): List<String> {
        val body = httpGet("https://api.openverse.org/v1/images/?q=${query.urlEncoded()}&page_size=$count")
            ?: return emptyList()
        return runCatching {
            json.decodeFromString<OpenverseSearchResponse>(body).results.map { it.url }
        }.getOrDefault(emptyList())
    }
}

class PexelsIosImageSource(
    private val apiKey: String,
) : RemoteImageSource {
    override suspend fun searchImageUrls(
        query: String,
        count: Int,
    ): List<String> {
        if (apiKey.isBlank()) return emptyList()
        val body = httpGet(
            url = "https://api.pexels.com/v1/search?query=${query.urlEncoded()}&per_page=$count",
            headers = mapOf("Authorization" to apiKey),
        ) ?: return emptyList()
        return runCatching {
            json.decodeFromString<PexelsSearchResponse>(body).photos.map { it.src.medium }
        }.getOrDefault(emptyList())
    }
}

class PixabayIosImageSource(
    private val apiKey: String,
) : RemoteImageSource {
    override suspend fun searchImageUrls(
        query: String,
        count: Int,
    ): List<String> {
        if (apiKey.isBlank()) return emptyList()
        val body = httpGet(
            "https://pixabay.com/api/?key=$apiKey&q=${query.urlEncoded()}&per_page=$count",
        ) ?: return emptyList()
        return runCatching {
            json.decodeFromString<PixabaySearchResponse>(body).hits.map { it.webformatURL }
        }.getOrDefault(emptyList())
    }
}
