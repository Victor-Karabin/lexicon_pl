package com.lexicon.data.repository

import com.lexicon.boundary.ImageProvider
import com.lexicon.data.remote.image.RemoteImageSource

class FallbackImageProviderImpl(
    private val sources: List<RemoteImageSource>,
) : ImageProvider {
    override suspend fun searchImage(query: String): String? {
        for (source in sources) {
            val url = source.searchImageUrl(query)
            if (!url.isNullOrBlank()) return url
        }
        return null
    }

    override suspend fun searchImages(
        query: String,
        count: Int,
        skip: Int,
    ): List<String> {
        val wanted = skip + count
        val pooled = LinkedHashSet<String>()
        for (source in sources) {
            source.searchImageUrls(query, wanted).forEach { url ->
                if (url.isNotBlank()) pooled += url
            }

            if (pooled.size >= wanted) break
        }
        return pooled.drop(skip).take(count)
    }

    override suspend fun pinImage(
        query: String,
        imageUrl: String,
    ) = Unit
}
