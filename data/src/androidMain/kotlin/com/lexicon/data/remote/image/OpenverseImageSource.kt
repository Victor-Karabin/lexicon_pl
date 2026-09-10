package com.lexicon.data.remote.image

private const val OPENVERSE_ANONYMOUS_MAX_PAGE_SIZE = 20

class OpenverseImageSource(
    private val api: OpenverseApi,
) : RemoteImageSource {
    override suspend fun searchImageUrls(
        query: String,
        count: Int,
    ): List<String> =
        runCatching {
            api.search(query, pageSize = count.coerceAtMost(OPENVERSE_ANONYMOUS_MAX_PAGE_SIZE)).results.map { it.url }
        }.getOrDefault(emptyList())
}
