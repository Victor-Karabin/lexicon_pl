package com.lexicon.data.remote.image

class PixabayImageSource(
    private val api: PixabayApi,
) : RemoteImageSource {
    override suspend fun searchImageUrl(query: String): String? =
        runCatching {
            api.search(query).hits.firstOrNull()?.webformatURL
        }.getOrNull()
}
