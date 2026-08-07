package com.lexicon.data.remote.image

import javax.inject.Inject

class PixabayImageSource
    @Inject
    constructor(
        private val api: PixabayApi,
    ) : RemoteImageSource {
        override suspend fun searchImageUrl(query: String): String? =
            runCatching {
                api.search(query).hits.firstOrNull()?.webformatURL
            }.getOrNull()
    }
