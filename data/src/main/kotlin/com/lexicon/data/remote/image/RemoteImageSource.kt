package com.lexicon.data.remote.image

/** One image search backend in the fallback chain. Never throws — a failure just means "no result". */
interface RemoteImageSource {
    suspend fun searchImageUrl(query: String): String?
}
