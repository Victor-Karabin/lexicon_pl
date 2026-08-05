package com.lexicon.pl.data.repository

import com.lexicon.pl.boundary.ImageProvider
import com.lexicon.pl.data.remote.image.OpenverseImageSource
import com.lexicon.pl.data.remote.image.PexelsImageSource
import com.lexicon.pl.data.remote.image.PixabayImageSource
import com.lexicon.pl.data.remote.image.UnsplashImageSource
import javax.inject.Inject

/** Tries each image source in order, falling through to the next on no result or failure. */
class FallbackImageProviderImpl
    @Inject
    constructor(
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
