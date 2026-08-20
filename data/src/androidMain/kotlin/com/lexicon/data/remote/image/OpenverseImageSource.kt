package com.lexicon.data.remote.image

class OpenverseImageSource(
    private val api: OpenverseApi,
) : RemoteImageSource {
    override suspend fun searchImageUrls(
        query: String,
        count: Int,
    ): List<String> =
        runCatching {
            api.search(query, pageSize = count).results.map { it.url }
        }.getOrDefault(emptyList())
}
