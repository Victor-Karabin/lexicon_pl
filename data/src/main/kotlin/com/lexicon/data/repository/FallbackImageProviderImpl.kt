package com.lexicon.data.repository

import com.lexicon.boundary.ImageProvider
import com.lexicon.data.remote.image.OpenverseImageSource
import com.lexicon.data.remote.image.PexelsImageSource
import com.lexicon.data.remote.image.PixabayImageSource
import com.lexicon.data.remote.image.UnsplashImageSource

class FallbackImageProviderImpl(
    private val pexels: PexelsImageSource,
    private val pixabay: PixabayImageSource,
    private val unsplash: UnsplashImageSource,
    private val openverse: OpenverseImageSource,
) : ImageProvider {
    override suspend fun searchImage(query: String): String? {
        val sources = listOf(pexels, pixabay, unsplash, openverse)
        for (source in sources) {
            val url = source.searchImageUrl(query)
            if (!url.isNullOrBlank()) return url
        }
        return null
    }
}
