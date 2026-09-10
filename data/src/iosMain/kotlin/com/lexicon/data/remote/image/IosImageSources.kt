package com.lexicon.data.remote.image

import com.lexicon.data.remote.httpGet
import com.lexicon.data.remote.urlEncoded
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

private const val OPENVERSE_ANONYMOUS_MAX_PAGE_SIZE = 20
private const val PEXELS_MAX_PER_PAGE = 80
private const val PIXABAY_MIN_PER_PAGE = 3
private const val PIXABAY_MAX_PER_PAGE = 200

class OpenverseIosImageSource : RemoteImageSource {
    override suspend fun searchImageUrls(
        query: String,
        count: Int,
    ): List<String> {
        val pageSize = count.coerceAtMost(OPENVERSE_ANONYMOUS_MAX_PAGE_SIZE)
        val body = httpGet("https://api.openverse.org/v1/images/?q=${query.urlEncoded()}&page_size=$pageSize")
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
            url = "https://api.pexels.com/v1/search?query=${query.urlEncoded()}&per_page=${count.coerceAtMost(PEXELS_MAX_PER_PAGE)}",
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
        val perPage = count.coerceIn(PIXABAY_MIN_PER_PAGE, PIXABAY_MAX_PER_PAGE)
        val body = httpGet(
            "https://pixabay.com/api/?key=$apiKey&q=${query.urlEncoded()}&per_page=$perPage",
        ) ?: return emptyList()
        return runCatching {
            json.decodeFromString<PixabaySearchResponse>(body).hits.map { it.webformatURL }
        }.getOrDefault(emptyList())
    }
}
