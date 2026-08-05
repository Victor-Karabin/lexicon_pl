package com.lexicon.pl.data.remote.image

import javax.inject.Inject

class OpenverseImageSource
    @Inject
    constructor(
        private val api: OpenverseApi,
    ) : RemoteImageSource {
        override suspend fun searchImageUrl(query: String): String? =
            runCatching {
                api.search(query).results.firstOrNull()?.url
            }.getOrNull()
    }
