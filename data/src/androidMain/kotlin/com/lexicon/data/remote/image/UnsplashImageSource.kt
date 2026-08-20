package com.lexicon.data.remote.image

class UnsplashImageSource(
    private val api: UnsplashApi,
) : RemoteImageSource {
    override suspend fun searchImageUrls(
        query: String,
        count: Int,
    ): List<String> =
        runCatching {
            api.search(query, perPage = count).results.map { it.urls.small }
        }.getOrDefault(emptyList())
}
