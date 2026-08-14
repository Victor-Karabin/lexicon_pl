package com.lexicon.data.repository

import com.lexicon.boundary.ImageProvider
import com.lexicon.data.local.ImageUrlCacheDao
import com.lexicon.data.local.ImageUrlCacheEntity

class CachingImageProviderImpl(
    private val delegate: FallbackImageProviderImpl,
    private val imageUrlCacheDao: ImageUrlCacheDao,
) : ImageProvider {
    override suspend fun searchImage(query: String): String? {
        imageUrlCacheDao.get(query)?.let { return it }

        val url = delegate.searchImage(query) ?: return null
        imageUrlCacheDao.insert(ImageUrlCacheEntity(query = query, imageUrl = url))
        return url
    }

    /**
     * Deliberately uncached: these are candidates to pick from, and the cache holds
     * one image per query — the chosen one, which [pinImage] writes.
     */
    override suspend fun searchImages(
        query: String,
        count: Int,
        skip: Int,
    ): List<String> = delegate.searchImages(query, count, skip)

    override suspend fun pinImage(
        query: String,
        imageUrl: String,
    ) = imageUrlCacheDao.insert(ImageUrlCacheEntity(query = query, imageUrl = imageUrl))
}
