package com.lexicon.data.remote.translate

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface MyMemoryApi {
    @GET("get")
    suspend fun translate(
        @Query("q") text: String,
        /** Source and target as `en|pl`. */
        @Query("langpair") langPair: String,
    ): MyMemoryResponse
}

@Serializable
data class MyMemoryResponse(
    @SerialName("responseData") val data: MyMemoryData? = null,
)

@Serializable
data class MyMemoryData(
    @SerialName("translatedText") val translatedText: String? = null,
)
