@file:OptIn(ExperimentalUuidApi::class)

package com.lexicon.domain.wordcard

import com.lexicon.boundary.ImageProvider
import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.boundary.TrainingResultOutcomeBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.common.Clock
import com.lexicon.domain.settings.StepCountResolver
import com.lexicon.interactors.wordcard.RecordWordCardSeenRequest
import com.lexicon.interactors.wordcard.RecordWordCardSeenUseCase
import com.lexicon.interactors.wordcard.StartWordCardSessionRequest
import com.lexicon.interactors.wordcard.StartWordCardSessionUseCase
import com.lexicon.interactors.wordcard.WordCardSessionResponse
import com.lexicon.interactors.wordcard.WordCardStep
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val WORD_CARD_TRAINING = "word_card"

/**
 * The deck for a session: as many words as a session is set to, from whatever list it
 * was given.
 *
 * The picture comes from [ImageProvider.searchImage], which answers from the cache
 * first — so a word shows the picture it was pinned with, and the picture trainings
 * later show the same one.
 */
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

/**
 * Records the card as seen rather than as answered.
 *
 * [TrainingResultOutcomeBoundary.SEEN] is what keeps this honest: the session and the
 * time it took are recorded, so a day of only word cards still counts as a day
 * studied and a program's turn at it completes — but the word's review schedule does
 * not move and accuracy does not shift, because nothing was asked.
 */
class RecordWordCardSeenUseCaseImpl(
    private val history: TrainingHistoryRepository,
    private val clock: Clock,
) : RecordWordCardSeenUseCase {
    override suspend fun invoke(request: RecordWordCardSeenRequest) {
        history.recordResult(
            TrainingResultBoundary(
                sessionId = request.sessionId,
                trainingType = WORD_CARD_TRAINING,
                stepIndex = request.stepIndex,
                vocabularyItemId = request.vocabularyItemId,
                expectedAnswer = request.text,
                submittedAnswer = "",
                outcome = TrainingResultOutcomeBoundary.SEEN,
                tipUsed = false,
                completedAtEpochMillis = clock.nowEpochMillis(),
            ),
        )
    }
}
