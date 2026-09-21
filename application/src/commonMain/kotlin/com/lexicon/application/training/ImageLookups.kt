package com.lexicon.application.training

import com.lexicon.boundary.ImageProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal suspend fun ImageProvider.picturesFor(queries: List<String>): List<String?> =
    coroutineScope {
        queries.map { query -> async { pictureOrNull(query) } }.awaitAll()
    }

private suspend fun ImageProvider.pictureOrNull(query: String): String? =
    try {
        searchImage(query)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
