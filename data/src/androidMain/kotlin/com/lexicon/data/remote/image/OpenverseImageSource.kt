package com.lexicon.data.remote.image

import android.util.Log

private const val TAG = "OpenverseImageSource"

private const val OPENVERSE_ANONYMOUS_MAX_PAGE_SIZE = 20

class OpenverseImageSource(
    private val api: OpenverseApi,
) : RemoteImageSource {
    override suspend fun searchImageUrls(
        query: String,
        count: Int,
    ): List<String> =
        runCatching {
            api.search(query, pageSize = count.coerceAtMost(OPENVERSE_ANONYMOUS_MAX_PAGE_SIZE)).results.map { it.url }
        }.onFailure { Log.w(TAG, "Openverse could not be searched", it) }
            .getOrThrow()
}
