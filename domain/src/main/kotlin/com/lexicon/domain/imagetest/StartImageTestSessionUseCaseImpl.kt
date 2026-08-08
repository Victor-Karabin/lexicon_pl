package com.lexicon.domain.imagetest

import com.lexicon.boundary.ImageProvider
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.domain.dictation.Word
import com.lexicon.domain.dictation.isPhrase
import com.lexicon.domain.dictation.toWord
import com.lexicon.domain.settings.StepCountResolver
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

/**
 * Distractors must match the subject's content type, so the pool has to be big enough to hold
 * enough same-type items — not merely enough items. A small pool can easily contain a single phrase,
 * leaving a phrase subject with no distractors at all and a one-option step.
 */
private const val MIN_POOL_SIZE = 60

class StartImageTestSessionUseCaseImpl
    @Inject
    constructor(
        private val vocabularyRepository: VocabularyRepository,
        private val imageProvider: ImageProvider,
        private val stepCountResolver: StepCountResolver,
    ) : StartImageTestSessionUseCase {
        override suspend fun invoke(request: StartImageTestSessionRequest): ImageTestSessionResponse {
            val stepCount = stepCountResolver.resolve(request.stepCount)
            val poolSize = maxOf(maxOf(stepCount, request.optionCount) * POOL_MULTIPLIER, MIN_POOL_SIZE)
            val pool = vocabularyRepository.getRandomItems(poolSize).map { it.toWord() }
            val subjects = subjectsWithEnoughDistractors(pool, request.optionCount).take(stepCount)

            val steps =
                coroutineScope {
                    subjects.mapIndexed { index, subject ->
                        async { buildStep(index, subject, pool, request.optionCount) }
                    }.map { it.await() }
                }
            return ImageTestSessionResponse(sessionId = UUID.randomUUID().toString(), steps = steps)
        }

        /**
         * Only picks subjects whose own content type has enough distinct options to fill the
         * answers. Vocabularies typically hold far fewer phrases than single words, so a phrase
         * subject would otherwise produce a step with one or two options — trivially guessable.
         * Falls back to the whole pool when no content type qualifies, which is the best available
         * rather than no step at all.
         */
        private fun subjectsWithEnoughDistractors(
            pool: List<Word>,
            optionCount: Int,
        ): List<Word> =
            pool.groupBy { it.isPhrase }
                .values
                .filter { sameType -> sameType.distinctBy(Word::text).size >= optionCount }
                .flatten()
                .ifEmpty { pool }

        /**
         * The options are target-language words: the point is recognising what the image is called
         * in the language being learnt, so offering base-language options would ask nothing.
         *
         * For the same reason the fallback clue is the base word — the target word is the answer,
         * and showing it when the image fails to load would give the step away.
         */
        private suspend fun buildStep(
            index: Int,
            subject: Word,
            pool: List<Word>,
            optionCount: Int,
        ): ImageTestStepResponse {
            val distractors =
                pool
                    .filter { it.id != subject.id && it.text != subject.text && it.isPhrase == subject.isPhrase }
                    .distinctBy { it.text }
                    .shuffled()
                    .take(optionCount - 1)
                    .map { it.text }
            val options = (distractors + subject.text).shuffled()

            return ImageTestStepResponse(
                stepIndex = index,
                vocabularyItemId = subject.id,
                imageUrl = imageProvider.searchImage(subject.translation),
                clueText = subject.translation,
                options = options,
                correctOption = subject.text,
            )
        }
    }
