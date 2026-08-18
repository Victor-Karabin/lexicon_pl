package com.lexicon.data.remote.sentence

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

interface OpenAiApi {
    @POST("v1/responses")
    suspend fun generate(
        @Body request: ResponsesRequest,
    ): ResponsesResult
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ResponsesRequest(
    val model: String,
    val input: List<ResponsesMessage>,
    @EncodeDefault val text: ResponsesText = ResponsesText(),
    @EncodeDefault val reasoning: ResponsesReasoning = ResponsesReasoning(),
    @EncodeDefault val tools: List<String> = emptyList(),
    @EncodeDefault val store: Boolean = true,
)

@Serializable
data class ResponsesMessage(
    val role: String,
    val content: List<ResponsesContent>,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ResponsesContent(
    @EncodeDefault val type: String = "input_text",
    val text: String,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ResponsesText(
    @EncodeDefault val format: ResponsesFormat = ResponsesFormat(),
    @EncodeDefault val verbosity: String = "medium",
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ResponsesFormat(
    @EncodeDefault val type: String = "text",
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ResponsesReasoning(
    @EncodeDefault val mode: String = "standard",
    @EncodeDefault val summary: String = "auto",
)

@Serializable
data class ResponsesResult(
    val output: List<ResponsesOutput> = emptyList(),
    @SerialName("output_text") val outputText: String? = null,
) {
    val sentence: String?
        get() = outputText?.takeIf { it.isNotBlank() }
            ?: output
                .asSequence()
                .flatMap { it.content.asSequence() }
                .mapNotNull { it.text }
                .firstOrNull { it.isNotBlank() }
}

@Serializable
data class ResponsesOutput(
    val type: String = "",
    val content: List<ResponsesOutputContent> = emptyList(),
)

@Serializable
data class ResponsesOutputContent(
    val type: String = "",
    val text: String? = null,
)
