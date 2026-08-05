package com.lexicon.pl.data.remote.image

import javax.inject.Inject

class PexelsImageSource
    @Inject
    constructor(
        private val api: PexelsApi,
    ) : RemoteImageSource {
        override suspend fun searchImageUrl(query: String): String? =
            runCatching {
                api.search(query).photos.firstOrNull()?.src?.medium
            }.getOrNull()
    }
