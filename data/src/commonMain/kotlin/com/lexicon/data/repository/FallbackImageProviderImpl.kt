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
}
