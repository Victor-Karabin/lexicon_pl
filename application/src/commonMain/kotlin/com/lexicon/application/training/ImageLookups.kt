package com.lexicon.application.training

import co.touchlab.kermit.Logger
import com.lexicon.boundary.ImageProvider
import com.lexicon.common.runSuspendCatching
import com.lexicon.model.vocabulary.Word
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private val log = Logger.withTag("ImageLookups")

internal suspend fun ImageProvider.picturesFor(words: List<Word>): List<String?> =
    coroutineScope {
        words.map { word -> async { pictureOf(word) } }.awaitAll()
    }

internal suspend fun ImageProvider.pictureOf(word: Word): String? = pictureOrNull(word.pictureSubject)

private suspend fun ImageProvider.pictureOrNull(query: String): String? =
    runSuspendCatching { searchImage(query) }
        .onFailure { log.w(it) { "The picture of a word could not be loaded" } }
        .getOrNull()
