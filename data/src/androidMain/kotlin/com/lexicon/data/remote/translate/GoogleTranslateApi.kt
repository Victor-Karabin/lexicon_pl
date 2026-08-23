package com.lexicon.data.remote.translate

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleTranslateApi {
    @GET("language/translate/v2")
    suspend fun translate(
        @Query("q") text: String,
        @Query("source") source: String,
        @Query("target") target: String,
        @Query("format") format: String = "text",
    ): GoogleTranslateResponse
}

@Serializable
data class GoogleTranslateResponse(val data: GoogleTranslateData? = null)

@Serializable
data class GoogleTranslateData(val translations: List<GoogleTranslation> = emptyList())

@Serializable
data class GoogleTranslation(val translatedText: String = "")
