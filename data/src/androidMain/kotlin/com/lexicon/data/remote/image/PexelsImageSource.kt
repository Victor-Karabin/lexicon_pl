package com.lexicon.data.remote.image

class PexelsImageSource(
    private val api: PexelsApi,
) : RemoteImageSource {
    override suspend fun searchImageUrls(
        query: String,
        count: Int,
    ): List<String> =
        runCatching {
            api.search(query, perPage = count).photos.map { it.src.medium }
        }.getOrDefault(emptyList())
}
