@file:OptIn(ExperimentalUuidApi::class)

package com.lexicon.application.puzzle

import com.lexicon.application.settings.StepCountResolver
import com.lexicon.application.training.open
import com.lexicon.boundary.ImageProvider
import com.lexicon.boundary.SessionStore
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.puzzle.PuzzleSessionResponse
import com.lexicon.interactors.puzzle.PuzzleStepResponse
import com.lexicon.interactors.puzzle.StartPuzzleSessionRequest
import com.lexicon.interactors.puzzle.StartPuzzleSessionUseCase
import com.lexicon.model.training.TrainingType
import com.lexicon.model.vocabulary.Word
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.uuid.ExperimentalUuidApi

class StartPuzzleSessionUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
    private val imageProvider: ImageProvider,
    private val stepCountResolver: StepCountResolver,
    private val sessions: SessionStore,
) : StartPuzzleSessionUseCase {
    override suspend fun invoke(request: StartPuzzleSessionRequest): PuzzleSessionResponse {
        val stepCount = stepCountResolver.resolve(request.stepCount)
        val words = vocabularyRepository.getRandomItems(stepCount, request.vocabularyIds).map { it }
        val steps = coroutineScope {
            words.mapIndexed { index, word ->
                async { buildStep(index, word) }
            }.awaitAll()
        }
        val sessionId = sessions.open(TrainingType.PUZZLE, words.map { it.id to it.text })
        return PuzzleSessionResponse(sessionId = sessionId.value, steps = steps)
    }

    private suspend fun buildStep(
        index: Int,
        word: Word,
    ): PuzzleStepResponse =
        PuzzleStepResponse(
            stepIndex = index,
            vocabularyItemId = word.id.value,
            expectedText = word.text,
            imageUrl = imageProvider.searchImage(word.translation),
            clueText = word.translation,
        )
}
