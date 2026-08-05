package com.lexicon.domain.trueorfalse

import com.lexicon.boundary.VocabularyRepository
import com.lexicon.domain.dictation.Word
import com.lexicon.domain.dictation.toWord
import com.lexicon.interactors.trueorfalse.StartTrueOrFalseSessionRequest
import com.lexicon.interactors.trueorfalse.StartTrueOrFalseSessionUseCase
import com.lexicon.interactors.trueorfalse.TrueOrFalseSessionResponse
import com.lexicon.interactors.trueorfalse.TrueOrFalseStepResponse
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

/** Fetches extra items beyond stepCount so every step has a candidate distractor to draw from. */
private const val DISTRACTOR_POOL_MULTIPLIER = 2

class StartTrueOrFalseSessionUseCaseImpl
    @Inject
    constructor(
        private val vocabularyRepository: VocabularyRepository,
    ) : StartTrueOrFalseSessionUseCase {
        override suspend fun invoke(request: StartTrueOrFalseSessionRequest): TrueOrFalseSessionResponse {
            val pool =
                vocabularyRepository
                    .getRandomItems(request.stepCount * DISTRACTOR_POOL_MULTIPLIER)
                    .map { it.toWord() }
            val subjects = pool.take(request.stepCount)

            val steps =
                subjects.mapIndexed { index, subject ->
                    val isCorrect = Random.nextDouble() < request.correctProbability
                    val displayedTranslation =
                        if (isCorrect) {
                            subject.translation
                        } else {
                            pickDistractorTranslation(subject, pool) ?: subject.translation
                        }
                    TrueOrFalseStepResponse(
                        stepIndex = index,
                        vocabularyItemId = subject.id,
                        word = subject.text,
                        displayedTranslation = displayedTranslation,
                        isDisplayedTranslationCorrect = displayedTranslation == subject.translation,
                    )
                }
            return TrueOrFalseSessionResponse(sessionId = UUID.randomUUID().toString(), steps = steps)
        }

        private fun pickDistractorTranslation(
            subject: Word,
            pool: List<Word>,
        ): String? =
            pool.filter { it.id != subject.id && it.translation != subject.translation }
                .randomOrNull()
                ?.translation
    }
