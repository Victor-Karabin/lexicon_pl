package com.lexicon.data.remote.image

class PexelsImageSource(
    private val api: PexelsApi,
) : RemoteImageSource {
    override suspend fun searchImageUrl(query: String): String? =
        runCatching {
            api.search(query).photos.firstOrNull()?.src?.medium
        }.getOrNull()
}
