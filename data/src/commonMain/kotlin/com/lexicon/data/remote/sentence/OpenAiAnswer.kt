package com.lexicon.data.remote.sentence

internal const val MODEL = "gpt-5.4-mini"

sealed interface OpenAiAnswer {
    data class Text(val text: String) : OpenAiAnswer

    data object Offline : OpenAiAnswer

    data class Failed(val reason: String) : OpenAiAnswer
}

fun openAiRequest(prompt: String): ResponsesRequest =
    ResponsesRequest(
        model = MODEL,
        input = listOf(
            ResponsesMessage(
                role = "developer",
                content = listOf(ResponsesContent(text = prompt)),
            ),
        ),
    )
