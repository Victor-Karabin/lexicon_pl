package com.lexicon.data.remote.image

private const val PIXABAY_MIN_PER_PAGE = 3
private const val PIXABAY_MAX_PER_PAGE = 200

class PixabayImageSource(
    private val api: PixabayApi,
) : RemoteImageSource {
    override suspend fun searchImageUrls(
        query: String,
        count: Int,
    ): List<String> =
        runCatching {
            api
                .search(query, perPage = count.coerceIn(PIXABAY_MIN_PER_PAGE, PIXABAY_MAX_PER_PAGE))
                .hits
                .map { it.webformatURL }
                .take(count)
        }.getOrDefault(emptyList())
}
