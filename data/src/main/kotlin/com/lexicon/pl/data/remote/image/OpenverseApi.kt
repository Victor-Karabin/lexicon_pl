package com.lexicon.pl.data.remote.image

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Called unauthenticated (public rate limit only) — last-resort fallback, so an OAuth2
 * client-credentials token flow wasn't worth building for this pass.
 */
interface OpenverseApi {
    @GET("v1/images/")
    suspend fun search(
        @Query("q") query: String,
        @Query("page_size") pageSize: Int = 1,
    ): OpenverseSearchResponse
}

@Serializable
data class OpenverseSearchResponse(val results: List<OpenverseImage> = emptyList())

@Serializable
data class OpenverseImage(val url: String)
