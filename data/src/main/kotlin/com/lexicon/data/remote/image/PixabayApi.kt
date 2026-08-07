package com.lexicon.data.remote.image

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface PixabayApi {
    @GET("api/")
    suspend fun search(
        @Query("q") query: String,
        // Pixabay requires per_page in [3, 200] even when only the first hit is used.
        @Query("per_page") perPage: Int = 3,
    ): PixabaySearchResponse
}

@Serializable
data class PixabaySearchResponse(val hits: List<PixabayHit> = emptyList())

@Serializable
data class PixabayHit(val webformatURL: String)
