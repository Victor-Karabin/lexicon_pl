package com.lexicon.data.remote.sentence

import com.lexicon.boundary.WritingReviewRequestBoundary
import com.lexicon.boundary.WritingReviewResultBoundary
import com.lexicon.boundary.WritingReviewer
import com.lexicon.data.remote.HttpReply
import com.lexicon.data.remote.httpPost
import kotlinx.serialization.json.Json

private const val OPENAI_RESPONSES_URL = "https://api.openai.com/v1/responses"

private val openAiJson = Json { ignoreUnknownKeys = true }

class IosWritingReviewer(
    private val apiKey: String,
) : WritingReviewer {
    override suspend fun review(request: WritingReviewRequestBoundary): WritingReviewResultBoundary {
        if (apiKey.isBlank()) return WritingReviewResultBoundary.Refused("no OpenAI key")
        return ask(writingReviewPrompt(request)).toReviewResult()
    }

    private suspend fun ask(prompt: String): OpenAiAnswer {
        val reply = httpPost(
            url = OPENAI_RESPONSES_URL,
            headers = mapOf("Authorization" to "Bearer $apiKey", "Content-Type" to "application/json"),
            body = openAiJson.encodeToString(ResponsesRequest.serializer(), openAiRequest(prompt)),
        )
        return when (reply) {
            is HttpReply.Ok ->
                runCatching { openAiJson.decodeFromString(ResponsesResult.serializer(), reply.body).sentence }
                    .getOrNull()
                    ?.let(OpenAiAnswer::Text)
                    ?: OpenAiAnswer.Failed("unreadable response")

            is HttpReply.Failed -> OpenAiAnswer.Failed("HTTP ${reply.status}")
            HttpReply.Offline -> OpenAiAnswer.Offline
        }
    }
}
