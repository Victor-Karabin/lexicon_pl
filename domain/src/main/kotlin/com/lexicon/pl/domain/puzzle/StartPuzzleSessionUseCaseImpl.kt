package com.lexicon.pl.domain.puzzle

import com.lexicon.pl.boundary.ImageProvider
import com.lexicon.pl.boundary.VocabularyRepository
import com.lexicon.pl.domain.dictation.Word
import com.lexicon.pl.domain.dictation.toWord
import com.lexicon.pl.interactors.puzzle.PuzzleSessionResponse
import com.lexicon.pl.interactors.puzzle.PuzzleStepResponse
import com.lexicon.pl.interactors.puzzle.StartPuzzleSessionRequest
import com.lexicon.pl.interactors.puzzle.StartPuzzleSessionUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.UUID
import javax.inject.Inject

class StartPuzzleSessionUseCaseImpl
    @Inject
    constructor(
        private val vocabularyRepository: VocabularyRepository,
        private val imageProvider: ImageProvider,
    ) : StartPuzzleSessionUseCase {
        override suspend fun invoke(request: StartPuzzleSessionRequest): PuzzleSessionResponse {
            val words = vocabularyRepository.getRandomItems(request.stepCount).map { it.toWord() }
            val steps =
                coroutineScope {
                    words.mapIndexed { index, word ->
                        async { buildStep(index, word) }
                    }.map { it.await() }
                }
            return PuzzleSessionResponse(sessionId = UUID.randomUUID().toString(), steps = steps)
        }

        private suspend fun buildStep(
            index: Int,
            word: Word,
        ): PuzzleStepResponse =
            PuzzleStepResponse(
                stepIndex = index,
                vocabularyItemId = word.id,
                expectedText = word.text,
                imageUrl = imageProvider.searchImage(word.translation),
                clueText = word.translation,
            )
    }
