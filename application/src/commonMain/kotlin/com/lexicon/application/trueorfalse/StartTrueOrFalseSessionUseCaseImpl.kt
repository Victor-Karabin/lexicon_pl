@file:OptIn(ExperimentalUuidApi::class)

package com.lexicon.application.trueorfalse

import com.lexicon.application.training.open
import com.lexicon.boundary.SessionStore
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.trueorfalse.StartTrueOrFalseSessionRequest
import com.lexicon.interactors.trueorfalse.StartTrueOrFalseSessionUseCase
import com.lexicon.interactors.trueorfalse.TrueOrFalseSessionResponse
import com.lexicon.interactors.trueorfalse.TrueOrFalseStepResponse
import com.lexicon.model.training.TrainingType
import com.lexicon.model.vocabulary.Word
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi

private const val DISTRACTOR_POOL_MULTIPLIER = 2

class StartTrueOrFalseSessionUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
    private val sessions: SessionStore,
) : StartTrueOrFalseSessionUseCase {
    override suspend fun invoke(request: StartTrueOrFalseSessionRequest): TrueOrFalseSessionResponse {
        val pool =
            vocabularyRepository
                .getRandomItems(request.poolSize * DISTRACTOR_POOL_MULTIPLIER, request.vocabularyIds)
                .map { it }
        val subjects = pool.take(request.poolSize)

        val steps =
            subjects.mapIndexed { index, subject ->
                val isCorrect = Random.nextDouble() < request.correctProbability
                val displayedTranslation = if (isCorrect) {
                    subject.translation
                } else {
                    pickDistractorTranslation(subject, pool) ?: subject.translation
                }
                TrueOrFalseStepResponse(
                    stepIndex = index,
                    vocabularyItemId = subject.id.value,
                    word = subject.text,
                    displayedTranslation = displayedTranslation,
                    isDisplayedTranslationCorrect = displayedTranslation == subject.translation,
                )
            }
        val sessionId = sessions.open(TrainingType.TRUE_OR_FALSE, subjects.map { it.id to it.translation })
        return TrueOrFalseSessionResponse(sessionId = sessionId.value, steps = steps)
    }

    private fun pickDistractorTranslation(
        subject: Word,
        pool: List<Word>,
    ): String? =
        pool.filter { it.id != subject.id && it.translation != subject.translation && it.isPhrase == subject.isPhrase }
            .randomOrNull()
            ?.translation
}
