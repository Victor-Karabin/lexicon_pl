package com.lexicon.data.remote.sentence

import kotlinx.coroutines.CancellationException
import java.io.IOException

suspend fun OpenAiApi.ask(prompt: String): OpenAiAnswer =
    try {
        generate(openAiRequest(prompt)).sentence?.let(OpenAiAnswer::Text) ?: OpenAiAnswer.Failed("empty response")
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        OpenAiAnswer.Offline
    } catch (e: Exception) {
        OpenAiAnswer.Failed(e.message ?: e::class.simpleName.orEmpty())
    }
