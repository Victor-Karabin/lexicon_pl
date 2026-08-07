package com.lexicon.data.remote.image

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface PexelsApi {
    @GET("v1/search")
    suspend fun search(
        @Query("query") query: String,
        @Query("per_page") perPage: Int = 1,
    ): PexelsSearchResponse
}

@Serializable
data class PexelsSearchResponse(val photos: List<PexelsPhoto> = emptyList())

@Serializable
data class PexelsPhoto(val src: PexelsPhotoSource)

@Serializable
data class PexelsPhotoSource(val medium: String)
