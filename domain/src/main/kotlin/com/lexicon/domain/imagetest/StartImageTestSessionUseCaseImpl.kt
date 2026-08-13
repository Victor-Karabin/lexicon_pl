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

private const val POOL_MULTIPLIER = 2

private const val MIN_POOL_SIZE = 60

class StartImageTestSessionUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
    private val imageProvider: ImageProvider,
    private val stepCountResolver: StepCountResolver,
) : StartImageTestSessionUseCase {
    override suspend fun invoke(request: StartImageTestSessionRequest): ImageTestSessionResponse {
        val stepCount = stepCountResolver.resolve(request.stepCount)
        val poolSize = maxOf(maxOf(stepCount, request.optionCount) * POOL_MULTIPLIER, MIN_POOL_SIZE)
        val pool = vocabularyRepository.getRandomItems(poolSize, request.vocabularyIds).map { it.toWord() }
        val subjects = subjectsWithEnoughDistractors(pool, request.optionCount).take(stepCount)

        val steps =
            coroutineScope {
                subjects.mapIndexed { index, subject ->
                    async { buildStep(index, subject, pool, request.optionCount) }
                }.map { it.await() }
            }
        return ImageTestSessionResponse(sessionId = UUID.randomUUID().toString(), steps = steps)
    }

    private fun subjectsWithEnoughDistractors(
        pool: List<Word>,
        optionCount: Int,
    ): List<Word> =
        pool.groupBy { it.isPhrase }
            .values
            .filter { sameType -> sameType.distinctBy(Word::text).size >= optionCount }
            .flatten()
            .ifEmpty { pool }

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
