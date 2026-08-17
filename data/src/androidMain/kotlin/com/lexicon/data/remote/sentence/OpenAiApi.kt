package com.lexicon.data.remote.sentence

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

@Serializable
data class ResponsesRequest(
    val model: String,
    val input: List<ResponsesMessage>,
    val text: ResponsesText = ResponsesText(),
    val reasoning: ResponsesReasoning = ResponsesReasoning(),
    val tools: List<String> = emptyList(),
    val store: Boolean = true,
)

@Serializable
data class ResponsesMessage(
    val role: String,
    val content: List<ResponsesContent>,
)

@Serializable
data class ResponsesContent(
    val type: String = "input_text",
    val text: String,
)

@Serializable
data class ResponsesText(
    val format: ResponsesFormat = ResponsesFormat(),
    val verbosity: String = "medium",
)

@Serializable
data class ResponsesFormat(val type: String = "text")

@Serializable
data class ResponsesReasoning(
    val mode: String = "standard",
    val summary: String = "auto",
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
