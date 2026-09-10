package com.lexicon.data.remote.image

private const val PEXELS_MAX_PER_PAGE = 80

class PexelsImageSource(
    private val api: PexelsApi,
) : RemoteImageSource {
    override suspend fun searchImageUrls(
        query: String,
        count: Int,
    ): List<String> =
        runCatching {
            api.search(query, perPage = count.coerceAtMost(PEXELS_MAX_PER_PAGE)).photos.map { it.src.medium }
        }.getOrDefault(emptyList())
}
