package com.lexicon.data.remote.translate

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface DeepLApi {
    @FormUrlEncoded
    @POST("v2/translate")
    suspend fun translate(
        @Field("text") text: String,
        @Field("source_lang") sourceLang: String,
        @Field("target_lang") targetLang: String,
    ): DeepLTranslateResponse
}

@Serializable
data class DeepLTranslateResponse(val translations: List<DeepLTranslation> = emptyList())

@Serializable
data class DeepLTranslation(
    val text: String,
    @SerialName("detected_source_language") val detectedSourceLanguage: String? = null,
)
