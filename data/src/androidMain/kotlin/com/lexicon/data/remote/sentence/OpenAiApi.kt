package com.lexicon.data.remote.sentence

import retrofit2.http.Body
import retrofit2.http.POST

interface OpenAiApi {
    @POST("v1/responses")
    suspend fun generate(
        @Body request: ResponsesRequest,
    ): ResponsesResult
}
