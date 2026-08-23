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

    /**
     * Takes from every source in turn rather than draining the first one that answers,
     * so a page is a spread of what the web has for the word instead of ten variations
     * from whichever provider happens to be listed first.
     */
    override suspend fun searchImages(
        query: String,
        count: Int,
        skip: Int,
    ): List<String> {
        val wanted = skip + count
        val perSource = sources.map { source ->
            runCatching { source.searchImageUrls(query, wanted) }
                .getOrDefault(emptyList())
                .filter { it.isNotBlank() }
        }

        val pooled = LinkedHashSet<String>()
        val deepest = perSource.maxOfOrNull { it.size } ?: 0
        for (rank in 0 until deepest) {
            for (fromOneSource in perSource) {
                fromOneSource.getOrNull(rank)?.let { pooled += it }
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
