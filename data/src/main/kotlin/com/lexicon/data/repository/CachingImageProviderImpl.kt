package com.lexicon.data.repository

import com.lexicon.boundary.ImageProvider
import com.lexicon.data.local.ImageUrlCacheDao
import com.lexicon.data.local.ImageUrlCacheEntity
import javax.inject.Inject

class CachingImageProviderImpl
    @Inject
    constructor(
        private val delegate: FallbackImageProviderImpl,
        private val imageUrlCacheDao: ImageUrlCacheDao,
    ) : ImageProvider {
        override suspend fun searchImage(query: String): String? {
            imageUrlCacheDao.get(query)?.let { return it }

            val url = delegate.searchImage(query) ?: return null
            imageUrlCacheDao.insert(ImageUrlCacheEntity(query = query, imageUrl = url))
            return url
        }
    }
