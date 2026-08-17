package com.lexicon.data.remote.image

const val IMAGE_CANDIDATE_COUNT = 3

interface RemoteImageSource {
    suspend fun searchImageUrls(
        query: String,
        count: Int,
    ): List<String>

    suspend fun searchImageUrl(query: String): String? = searchImageUrls(query, count = 1).firstOrNull()
}
