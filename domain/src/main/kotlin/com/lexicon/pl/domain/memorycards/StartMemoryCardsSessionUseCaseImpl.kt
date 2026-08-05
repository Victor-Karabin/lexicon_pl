package com.lexicon.pl.domain.memorycards

import com.lexicon.pl.boundary.ImageProvider
import com.lexicon.pl.boundary.VocabularyRepository
import com.lexicon.pl.domain.dictation.Word
import com.lexicon.pl.domain.dictation.toWord
import com.lexicon.pl.interactors.memorycards.MemoryCardsPairResponse
import com.lexicon.pl.interactors.memorycards.MemoryCardsSessionResponse
import com.lexicon.pl.interactors.memorycards.MemoryCardsStepResponse
import com.lexicon.pl.interactors.memorycards.StartMemoryCardsSessionRequest
import com.lexicon.pl.interactors.memorycards.StartMemoryCardsSessionUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.UUID
import javax.inject.Inject

class StartMemoryCardsSessionUseCaseImpl
    @Inject
    constructor(
        private val vocabularyRepository: VocabularyRepository,
        private val imageProvider: ImageProvider,
    ) : StartMemoryCardsSessionUseCase {
        override suspend fun invoke(request: StartMemoryCardsSessionRequest): MemoryCardsSessionResponse {
            val steps =
                coroutineScope {
                    (0 until request.stepCount).map { stepIndex ->
                        async { buildStep(stepIndex, request.pairsPerStep) }
                    }.map { it.await() }
                }
            return MemoryCardsSessionResponse(sessionId = UUID.randomUUID().toString(), steps = steps)
        }

        private suspend fun buildStep(
            stepIndex: Int,
            pairsPerStep: Int,
        ): MemoryCardsStepResponse {
            val words = vocabularyRepository.getRandomItems(pairsPerStep).map { it.toWord() }
            val pairs =
                coroutineScope {
                    words.map { word -> async { buildPair(word) } }.map { it.await() }
                }
            return MemoryCardsStepResponse(stepIndex = stepIndex, pairs = pairs)
        }

        private suspend fun buildPair(word: Word): MemoryCardsPairResponse =
            MemoryCardsPairResponse(
                vocabularyItemId = word.id,
                imageUrl = imageProvider.searchImage(word.translation),
                imageFallbackText = word.text,
                text = word.translation,
            )
    }
