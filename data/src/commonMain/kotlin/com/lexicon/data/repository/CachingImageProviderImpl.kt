package com.lexicon.data.repository

import com.lexicon.boundary.ImageProvider
import com.lexicon.common.Clock
import com.lexicon.data.local.ImageUrlCacheDao
import com.lexicon.data.local.ImageUrlCacheEntity
import com.lexicon.data.local.NO_IMAGE

private const val NO_IMAGE_RECHECK_MILLIS = 7L * 24 * 60 * 60 * 1000

class CachingImageProviderImpl(
    private val delegate: FallbackImageProviderImpl,
    private val imageUrlCacheDao: ImageUrlCacheDao,
    private val clock: Clock,
) : ImageProvider {
    override suspend fun searchImage(query: String): String? {
        val cached = imageUrlCacheDao.find(query)
        when {
            cached == null -> Unit
            cached.imageUrl != NO_IMAGE -> return cached.imageUrl
            clock.nowEpochMillis() - cached.checkedAtEpochMillis < NO_IMAGE_RECHECK_MILLIS -> return null
        }

        return when (val lookup = delegate.lookUp(query)) {
            is ImageLookup.Found -> lookup.url.also { remember(query, it) }
            ImageLookup.NoneFound -> null.also { remember(query, NO_IMAGE) }
            ImageLookup.Unavailable -> null
        }
    }

    override suspend fun searchImages(
        query: String,
        count: Int,
        skip: Int,
    ): List<String> = delegate.searchImages(query, count, skip)

    override suspend fun pinImage(
        query: String,
        imageUrl: String,
    ) = remember(query, imageUrl)

    private suspend fun remember(
        query: String,
        imageUrl: String,
    ) = imageUrlCacheDao.insert(
        ImageUrlCacheEntity(query = query, imageUrl = imageUrl, checkedAtEpochMillis = clock.nowEpochMillis()),
    )
}
