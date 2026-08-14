package com.lexicon.data.remote.image

class OpenverseImageSource(
    private val api: OpenverseApi,
) : RemoteImageSource {
    override suspend fun searchImageUrl(query: String): String? =
        runCatching {
            api.search(query).results.firstOrNull()?.url
        }.getOrNull()
}
