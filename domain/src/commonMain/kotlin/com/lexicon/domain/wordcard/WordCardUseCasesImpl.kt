@file:OptIn(ExperimentalUuidApi::class)

package com.lexicon.domain.wordcard

import com.lexicon.boundary.ImageProvider
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.domain.settings.StepCountResolver
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.interactors.wordcard.RecordWordCardSeenRequest
import com.lexicon.interactors.wordcard.RecordWordCardSeenUseCase
import com.lexicon.interactors.wordcard.StartWordCardSessionRequest
import com.lexicon.interactors.wordcard.StartWordCardSessionUseCase
import com.lexicon.interactors.wordcard.WordCardSessionResponse
import com.lexicon.interactors.wordcard.WordCardStep
import com.lexicon.model.training.StepOutcome
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val WORD_CARD_TRAINING = "word_card"

class StartWordCardSessionUseCaseImpl(
    private val vocabulary: VocabularyRepository,
    private val imageProvider: ImageProvider,
    private val stepCountResolver: StepCountResolver,
) : StartWordCardSessionUseCase {
    override suspend fun invoke(request: StartWordCardSessionRequest): WordCardSessionResponse {
        val stepCount = stepCountResolver.resolve(null)
        val words = vocabulary.getRandomItems(stepCount, request.vocabularyIds)

        val steps = words.mapIndexed { index, word ->
            WordCardStep(
                stepIndex = index,
                vocabularyItemId = word.id,
                text = word.text,
                translation = word.translation,
                transcription = word.transcription,
                imageUrl = runCatching { imageProvider.searchImage(word.translation) }.getOrNull(),
            )
        }
        return WordCardSessionResponse(sessionId = Uuid.random().toString(), steps = steps)
    }
}

class RecordWordCardSeenUseCaseImpl(
    private val recordAnswer: RecordAnswerUseCase,
) : RecordWordCardSeenUseCase {
    override suspend fun invoke(request: RecordWordCardSeenRequest) {
        recordAnswer(
            RecordedAnswer(
                sessionId = request.sessionId,
                trainingType = WORD_CARD_TRAINING,
                stepIndex = request.stepIndex,
                vocabularyItemId = request.vocabularyItemId,
                expectedAnswer = request.text,
                submittedAnswer = "",
                outcome = StepOutcome.SEEN,
                tipUsed = false,
            ),
        )
    }
}
