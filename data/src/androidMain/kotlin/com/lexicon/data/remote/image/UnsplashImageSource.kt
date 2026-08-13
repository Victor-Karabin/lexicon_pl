package com.lexicon.data.remote.image

class UnsplashImageSource(
    private val api: UnsplashApi,
) : RemoteImageSource {
    override suspend fun searchImageUrl(query: String): String? =
        runCatching {
            api.search(query).results.firstOrNull()?.urls?.small
        }.getOrNull()
}
