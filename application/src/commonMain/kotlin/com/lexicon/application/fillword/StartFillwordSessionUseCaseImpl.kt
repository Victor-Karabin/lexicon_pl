package com.lexicon.application.fillword

import com.lexicon.application.training.open
import com.lexicon.application.training.wordsToStudy
import com.lexicon.boundary.SessionStore
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.fillword.FillwordPuzzle
import com.lexicon.interactors.fillword.FillwordSessionResult
import com.lexicon.interactors.fillword.StartFillwordSessionRequest
import com.lexicon.interactors.fillword.StartFillwordSessionUseCase
import com.lexicon.model.training.TrainingType
import com.lexicon.model.vocabulary.Word
import kotlin.random.Random

private const val GRID_SIZE = 10

private const val MAX_WORDS = 12

private const val MIN_WORD_LENGTH = 3

private const val MAX_LETTERS = GRID_SIZE * GRID_SIZE / 2

private const val PACKING_ATTEMPTS = 12

class StartFillwordSessionUseCaseImpl(
    private val vocabulary: VocabularyRepository,
    private val sessions: SessionStore,
) : StartFillwordSessionUseCase {
    override suspend fun invoke(request: StartFillwordSessionRequest): FillwordSessionResult {
        val chosen = vocabulary.wordsToStudy(request.vocabularyIds)
            .filter { word -> word.gridForm.none(Char::isWhitespace) && word.gridForm.length in MIN_WORD_LENGTH..GRID_SIZE }
            .distinctBy { it.gridForm }
            .shuffled()
            .fillGrid()
        if (chosen.isEmpty()) return FillwordSessionResult.EmptyStudySet

        val words = chosen.map { it.gridForm }
        val translations = chosen.associate { it.gridForm to it.translation }

        var best = pack(words, translations)
        repeat(PACKING_ATTEMPTS - 1) {
            if (best.words.size < words.size) {
                val attempt = pack(words, translations)
                if (attempt.words.size > best.words.size) best = attempt
            }
        }

        val byGridForm = chosen.associateBy { it.gridForm }
        val sessionId = sessions.open(
            TrainingType.FILLWORD,
            best.words.map { placed -> byGridForm.getValue(placed.word).let { it.id to it.text } },
        )

        return FillwordSessionResult.Ready(sessionId = sessionId.value, puzzle = best)
    }

    private fun List<Word>.fillGrid(): List<Word> {
        var letters = 0
        return takeWhile { word ->
            letters += word.gridForm.length
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

    private val Word.gridForm: String get() = text.uppercase()
}
