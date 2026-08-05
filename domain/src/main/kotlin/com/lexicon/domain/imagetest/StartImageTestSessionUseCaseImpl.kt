package com.lexicon.domain.imagetest

import com.lexicon.boundary.ImageProvider
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.domain.dictation.Word
import com.lexicon.domain.dictation.toWord
import com.lexicon.interactors.imagetest.ImageTestSessionResponse
import com.lexicon.interactors.imagetest.ImageTestStepResponse
import com.lexicon.interactors.imagetest.StartImageTestSessionRequest
import com.lexicon.interactors.imagetest.StartImageTestSessionUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.UUID
import javax.inject.Inject

/** Fetched pool is oversized so every step has enough distinct translations to draw distractors from. */
private const val POOL_MULTIPLIER = 2

class StartImageTestSessionUseCaseImpl
    @Inject
    constructor(
        private val vocabularyRepository: VocabularyRepository,
        private val imageProvider: ImageProvider,
    ) : StartImageTestSessionUseCase {
        override suspend fun invoke(request: StartImageTestSessionRequest): ImageTestSessionResponse {
            val poolSize = maxOf(request.stepCount, request.optionCount) * POOL_MULTIPLIER
            val pool = vocabularyRepository.getRandomItems(poolSize).map { it.toWord() }
            val subjects = pool.take(request.stepCount)

            val steps =
                coroutineScope {
                    subjects.mapIndexed { index, subject ->
                        async { buildStep(index, subject, pool, request.optionCount) }
                    }.map { it.await() }
                }
            return ImageTestSessionResponse(sessionId = UUID.randomUUID().toString(), steps = steps)
        }

        private suspend fun buildStep(
            index: Int,
            subject: Word,
            pool: List<Word>,
            optionCount: Int,
        ): ImageTestStepResponse {
            val distractors =
                pool
                    .filter { it.id != subject.id && it.translation != subject.translation }
                    .distinctBy { it.translation }
                    .shuffled()
                    .take(optionCount - 1)
                    .map { it.translation }
            val options = (distractors + subject.translation).shuffled()

            return ImageTestStepResponse(
                stepIndex = index,
                vocabularyItemId = subject.id,
                imageUrl = imageProvider.searchImage(subject.translation),
                clueText = subject.text,
                options = options,
                correctOption = subject.translation,
            )
        }
    }
