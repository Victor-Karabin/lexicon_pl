package com.lexicon.data.repository

import com.lexicon.boundary.ImageProvider
import com.lexicon.data.remote.image.RemoteImageSource

/**
 * Asks each source in turn and takes the first hit.
 *
 * The sources arrive as an ordered list rather than four named constructor
 * parameters: which image APIs exist is a per-platform question (today only the
 * Android target has any), so the platform's DI module decides both the set and
 * the fallback order.
 */
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
     * Pools every source rather than stopping at the first that answers, which is
     * what [searchImage] does. Choosing between four pictures of the same thing from
     * one stock library is a poorer choice than four from four, and asking again for
     * [skip] more only turns up something new if there is a deeper pool to draw on.
     */
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
            // Sources are ordered by preference, so stop as soon as the better ones
            // have covered the ask rather than calling every API every time.
            if (pooled.size >= wanted) break
        }
        return pooled.drop(skip).take(count)
    }

    /** Nothing to pin against: this provider has no store. [CachingImageProviderImpl] has. */
    override suspend fun pinImage(
        query: String,
        imageUrl: String,
    ) = Unit
}
