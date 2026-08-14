package com.lexicon.data.remote.image

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenverseApi {
    @GET("v1/images/")
    suspend fun search(
        @Query("q") query: String,
        @Query("page_size") pageSize: Int = IMAGE_CANDIDATE_COUNT,
    ): OpenverseSearchResponse
}

@Serializable
data class OpenverseSearchResponse(val results: List<OpenverseImage> = emptyList())

@Serializable
data class OpenverseImage(val url: String)
