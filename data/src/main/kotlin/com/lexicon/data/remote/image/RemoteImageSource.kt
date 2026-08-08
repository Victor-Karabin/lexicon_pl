package com.lexicon.data.remote.image

interface RemoteImageSource {
    suspend fun searchImageUrl(query: String): String?
}
