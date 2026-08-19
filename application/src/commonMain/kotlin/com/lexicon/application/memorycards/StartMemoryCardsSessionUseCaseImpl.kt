@file:OptIn(ExperimentalUuidApi::class)

package com.lexicon.application.memorycards

import com.lexicon.application.settings.StepCountResolver
import com.lexicon.application.training.openBoards
import com.lexicon.boundary.ImageProvider
import com.lexicon.boundary.SessionStore
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.memorycards.MemoryCardsPairResponse
import com.lexicon.interactors.memorycards.MemoryCardsSessionResponse
import com.lexicon.interactors.memorycards.MemoryCardsStepResponse
import com.lexicon.interactors.memorycards.StartMemoryCardsSessionRequest
import com.lexicon.interactors.memorycards.StartMemoryCardsSessionUseCase
import com.lexicon.model.training.TrainingType
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.uuid.ExperimentalUuidApi

class StartMemoryCardsSessionUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
    private val imageProvider: ImageProvider,
    private val stepCountResolver: StepCountResolver,
    private val sessions: SessionStore,
) : StartMemoryCardsSessionUseCase {
    override suspend fun invoke(request: StartMemoryCardsSessionRequest): MemoryCardsSessionResponse {
        val stepCount = stepCountResolver.resolve(request.stepCount)
        val steps =
            coroutineScope {
                (0 until stepCount).map { stepIndex ->
                    async { buildStep(stepIndex, request.pairsPerStep, request.vocabularyIds) }
                }.map { it.await() }
            }
        val sessionId = sessions.openBoards(
            training = TrainingType.MEMORY_CARDS,
            boards = steps.map { step -> step.pairs.map { VocabularyId(it.vocabularyItemId) } },
        )
        return MemoryCardsSessionResponse(sessionId = sessionId.value, steps = steps)
    }

    private suspend fun buildStep(
        stepIndex: Int,
        pairsPerStep: Int,
        vocabularyIds: List<Long>,
    ): MemoryCardsStepResponse {
        val words = vocabularyRepository.getRandomItems(pairsPerStep, vocabularyIds).map { it }
        val pairs =
            coroutineScope {
                words.map { word -> async { buildPair(word) } }.map { it.await() }
            }
        return MemoryCardsStepResponse(stepIndex = stepIndex, pairs = pairs)
    }

    private suspend fun buildPair(word: Word): MemoryCardsPairResponse =
        MemoryCardsPairResponse(
            vocabularyItemId = word.id.value,
            imageUrl = imageProvider.searchImage(word.translation),
            imageFallbackText = word.text,
            text = word.translation,
        )
}
