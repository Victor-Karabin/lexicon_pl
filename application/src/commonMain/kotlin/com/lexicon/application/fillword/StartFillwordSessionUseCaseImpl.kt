package com.lexicon.application.fillword

import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.fillword.FillwordPuzzle
import com.lexicon.interactors.fillword.FillwordSessionResult
import com.lexicon.interactors.fillword.StartFillwordSessionUseCase
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val GRID_SIZE = 10

private const val MAX_WORDS = 12

private const val MIN_WORD_LENGTH = 3

private const val MAX_LETTERS = GRID_SIZE * GRID_SIZE / 2

private const val PACKING_ATTEMPTS = 12

class StartFillwordSessionUseCaseImpl(
    private val vocabulary: VocabularyRepository,
) : StartFillwordSessionUseCase {
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun invoke(): FillwordSessionResult {
        val studySet = vocabulary.getItemsByIds(vocabulary.studySetWordIds())
        if (studySet.isEmpty()) return FillwordSessionResult.EmptyStudySet

        val chosen = studySet
            .map { it.text.uppercase() to it.translation }
            .filter { (text, _) -> text.none(Char::isWhitespace) && text.length in MIN_WORD_LENGTH..GRID_SIZE }
            .distinctBy { (text, _) -> text }
            .shuffled()
            .fillGrid()
        if (chosen.isEmpty()) return FillwordSessionResult.EmptyStudySet

        val words = chosen.map { (text, _) -> text }
        val translations = chosen.toMap()

        var best = pack(words, translations)
        repeat(PACKING_ATTEMPTS - 1) {
            if (best.words.size < words.size) {
                val attempt = pack(words, translations)
                if (attempt.words.size > best.words.size) best = attempt
            }
        }

        return FillwordSessionResult.Ready(sessionId = Uuid.random().toString(), puzzle = best)
    }

    private fun List<Pair<String, String>>.fillGrid(): List<Pair<String, String>> {
        var letters = 0
        return takeWhile { (word, _) ->
            letters += word.length
            letters <= MAX_LETTERS
        }.take(MAX_WORDS)
    }

    private fun pack(
        words: List<String>,
        translations: Map<String, String>,
    ): FillwordPuzzle {
        val builder = FillwordGrid(size = GRID_SIZE, random = Random.Default)
        words.sortedByDescending { it.length }.forEach(builder::add)
        return builder.toPuzzle(translations)
    }
}
