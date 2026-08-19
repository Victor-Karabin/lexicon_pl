@file:OptIn(ExperimentalUuidApi::class)

package com.lexicon.domain.dictation

import com.lexicon.boundary.SessionStore
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.domain.settings.StepCountResolver
import com.lexicon.interactors.dictation.DictationSessionResponse
import com.lexicon.interactors.dictation.DictationStepResponse
import com.lexicon.interactors.dictation.StartDictationSessionRequest
import com.lexicon.interactors.dictation.StartDictationSessionUseCase
import com.lexicon.model.training.Session
import com.lexicon.model.training.SessionId
import com.lexicon.model.training.Step
import com.lexicon.model.training.TrainingType
import kotlinx.collections.immutable.toImmutableList
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class StartDictationSessionUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
    private val stepCountResolver: StepCountResolver,
    private val sessions: SessionStore,
) : StartDictationSessionUseCase {
    override suspend fun invoke(request: StartDictationSessionRequest): DictationSessionResponse {
        val stepCount = stepCountResolver.resolve(request.stepCount)
        val words = vocabularyRepository.getRandomItems(stepCount, request.vocabularyIds)

        val sessionId = SessionId(Uuid.random().toString())

        if (words.isNotEmpty()) {
            sessions.save(
                Session(
                    id = sessionId,
                    training = TrainingType.DICTATION,
                    steps = words.mapIndexed { index, word ->
                        Step(index = index, wordId = word.id, expectedAnswer = word.text)
                    }.toImmutableList(),
                ),
            )
        }

        return DictationSessionResponse(
            sessionId = sessionId.value,
            steps = words.mapIndexed { index, word ->
                DictationStepResponse(
                    stepIndex = index,
                    vocabularyItemId = word.id.value,
                    expectedText = word.text,
                    translationText = word.translation,
                )
            },
        )
    }
}
