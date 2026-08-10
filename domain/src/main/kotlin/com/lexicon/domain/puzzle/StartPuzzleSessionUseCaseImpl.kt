package com.lexicon.domain.puzzle

import com.lexicon.boundary.ImageProvider
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.domain.dictation.Word
import com.lexicon.domain.dictation.toWord
import com.lexicon.domain.settings.StepCountResolver
import com.lexicon.interactors.puzzle.PuzzleSessionResponse
import com.lexicon.interactors.puzzle.PuzzleStepResponse
import com.lexicon.interactors.puzzle.StartPuzzleSessionRequest
import com.lexicon.interactors.puzzle.StartPuzzleSessionUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.UUID
import javax.inject.Inject

class StartPuzzleSessionUseCaseImpl
    @Inject
    constructor(
        private val vocabularyRepository: VocabularyRepository,
        private val imageProvider: ImageProvider,
        private val stepCountResolver: StepCountResolver,
    ) : StartPuzzleSessionUseCase {
        override suspend fun invoke(request: StartPuzzleSessionRequest): PuzzleSessionResponse {
            val stepCount = stepCountResolver.resolve(request.stepCount)
            val words = vocabularyRepository.getRandomItems(stepCount, request.vocabularyIds).map { it.toWord() }
            val steps = coroutineScope {
                words.mapIndexed { index, word ->
                    async { buildStep(index, word) }
                }.awaitAll()
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
