@file:OptIn(ExperimentalUuidApi::class)

package com.lexicon.application.memorycards

import com.lexicon.application.settings.StepCountResolver
import com.lexicon.boundary.ImageProvider
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.memorycards.MemoryCardsPairResponse
import com.lexicon.interactors.memorycards.MemoryCardsSessionResponse
import com.lexicon.interactors.memorycards.MemoryCardsStepResponse
import com.lexicon.interactors.memorycards.StartMemoryCardsSessionRequest
import com.lexicon.interactors.memorycards.StartMemoryCardsSessionUseCase
import com.lexicon.model.vocabulary.Word
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class StartMemoryCardsSessionUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
    private val imageProvider: ImageProvider,
    private val stepCountResolver: StepCountResolver,
) : StartMemoryCardsSessionUseCase {
    override suspend fun invoke(request: StartMemoryCardsSessionRequest): MemoryCardsSessionResponse {
        val stepCount = stepCountResolver.resolve(request.stepCount)
        val steps =
            coroutineScope {
                (0 until stepCount).map { stepIndex ->
                    async { buildStep(stepIndex, request.pairsPerStep, request.vocabularyIds) }
                }.map { it.await() }
            }
        return MemoryCardsSessionResponse(sessionId = Uuid.random().toString(), steps = steps)
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
