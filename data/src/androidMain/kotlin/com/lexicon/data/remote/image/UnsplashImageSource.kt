package com.lexicon.data.remote.image

private const val UNSPLASH_MAX_PER_PAGE = 30

class UnsplashImageSource(
    private val api: UnsplashApi,
) : RemoteImageSource {
    override suspend fun searchImageUrls(
        query: String,
        count: Int,
    ): List<String> =
        runCatching {
            api.search(query, perPage = count.coerceAtMost(UNSPLASH_MAX_PER_PAGE)).results.map { it.urls.small }
        }.getOrDefault(emptyList())
}
