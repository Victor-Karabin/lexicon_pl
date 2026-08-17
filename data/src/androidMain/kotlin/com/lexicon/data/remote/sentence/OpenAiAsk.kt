package com.lexicon.data.remote.sentence

import kotlinx.coroutines.CancellationException
import java.io.IOException

internal const val MODEL = "gpt-5.4-mini"

sealed interface OpenAiAnswer {
    data class Text(val text: String) : OpenAiAnswer

    data object Offline : OpenAiAnswer

    data class Failed(val reason: String) : OpenAiAnswer
}

suspend fun OpenAiApi.ask(prompt: String): OpenAiAnswer =
    try {
        val result = generate(
            ResponsesRequest(
                model = MODEL,
                input = listOf(
                    ResponsesMessage(
                        role = "developer",
                        content = listOf(ResponsesContent(text = prompt)),
                    ),
                ),
            ),
        )
        result.sentence?.let(OpenAiAnswer::Text) ?: OpenAiAnswer.Failed("empty response")
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        OpenAiAnswer.Offline
    } catch (e: Exception) {
        OpenAiAnswer.Failed(e.message ?: e::class.simpleName.orEmpty())
    }
