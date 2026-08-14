package com.lexicon.data.remote.image

/** How many images the learner is offered to choose between at a time. */
const val IMAGE_CANDIDATE_COUNT = 3

interface RemoteImageSource {
    /**
     * Up to [count] candidates for [query], best first, or an empty list if the
     * source has nothing or cannot be reached. Fewer than [count] is normal — an
     * obscure word may only match once.
     */
    suspend fun searchImageUrls(
        query: String,
        count: Int,
    ): List<String>

    /** The single best match, which is what the trainings ask for. */
    suspend fun searchImageUrl(query: String): String? = searchImageUrls(query, count = 1).firstOrNull()
}
