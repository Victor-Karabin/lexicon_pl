package com.lexicon.application.training

import com.lexicon.boundary.ImageProvider
import com.lexicon.model.vocabulary.Word
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal suspend fun ImageProvider.picturesFor(words: List<Word>): List<String?> =
    coroutineScope {
        words.map { word -> async { pictureOf(word) } }.awaitAll()
    }

internal suspend fun ImageProvider.pictureOf(word: Word): String? = word.pictureSubject?.let { pictureOrNull(it) }

private suspend fun ImageProvider.pictureOrNull(query: String): String? =
    try {
        searchImage(query)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
