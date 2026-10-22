package com.lexicon.application.training

import com.lexicon.boundary.SentenceGenerator
import com.lexicon.boundary.SentenceRequestBoundary
import com.lexicon.boundary.SentenceResultBoundary
import com.lexicon.model.vocabulary.CefrLevel
import com.lexicon.model.vocabulary.Word
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal fun List<Word>.maxLevel(): String = mapNotNull { it.cefr }.maxByOrNull { it.ordinal }?.name ?: CefrLevel.A1.name

internal suspend fun SentenceGenerator.sentencesFor(
    words: List<Word>,
    level: String,
): List<Pair<Word, SentenceResultBoundary>> =
    coroutineScope {
        words
            .map { word ->
                async {
                    word to generate(
                        SentenceRequestBoundary(
                            word = word.text,
                            translation = word.translation,
                            level = level,
                            context = "",
                            requiredWords = emptyList(),
                        ),
                    )
                }
            }.awaitAll()
    }

internal fun List<SentenceResultBoundary>.failure(): SentenceResultBoundary? =
    firstOrNull { it is SentenceResultBoundary.Offline } ?: firstOrNull { it is SentenceResultBoundary.Refused }
