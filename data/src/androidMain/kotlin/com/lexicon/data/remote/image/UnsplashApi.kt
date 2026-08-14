package com.lexicon.data.remote.image

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface UnsplashApi {
    @GET("search/photos")
    suspend fun search(
        @Query("query") query: String,
        @Query("per_page") perPage: Int = IMAGE_CANDIDATE_COUNT,
    ): UnsplashSearchResponse
}

@Serializable
data class UnsplashSearchResponse(val results: List<UnsplashPhoto> = emptyList())

@Serializable
data class UnsplashPhoto(val urls: UnsplashPhotoUrls)

@Serializable
data class UnsplashPhotoUrls(val small: String)
