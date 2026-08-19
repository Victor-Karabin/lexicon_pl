package com.lexicon.application.training

import com.lexicon.boundary.ReviewScheduleRepository
import com.lexicon.boundary.StudyRecordRepository
import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.common.Clock
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.model.scheduling.ReviewSettings
import com.lexicon.model.scheduling.ReviewState
import com.lexicon.model.scheduling.StudyTimePolicy
import com.lexicon.model.scheduling.next
import com.lexicon.model.scheduling.recallQuality

class RecordAnswerUseCaseImpl(
    private val history: TrainingHistoryRepository,
    private val reviews: ReviewScheduleRepository,
    private val studyRecord: StudyRecordRepository,
    private val clock: Clock,
    private val reviewSettings: ReviewSettings = ReviewSettings(),
    private val studyTimePolicy: StudyTimePolicy = StudyTimePolicy(),
) : RecordAnswerUseCase {
    override suspend fun invoke(answer: RecordedAnswer) {
        val schedule = reviews.find(answer.vocabularyItemId)
        val wasReview = schedule != null

        val answeredAtEpochMillis = clock.nowEpochMillis()
        val previousAnswerAtEpochMillis = history.lastAnsweredAtEpochMillis()

        history.recordResult(
            TrainingResultBoundary(
                sessionId = answer.sessionId,
                trainingType = answer.trainingType,
                stepIndex = answer.stepIndex,
                vocabularyItemId = answer.vocabularyItemId,
                expectedAnswer = answer.expectedAnswer,
                submittedAnswer = answer.submittedAnswer,
                outcome = answer.outcome,
                tipUsed = answer.tipUsed,
                completedAtEpochMillis = answeredAtEpochMillis,
                wasReview = wasReview,
            ),
        )

        val todayEpochDay = clock.todayEpochDay()

        val quality = answer.outcome.recallQuality(answer.tipUsed)
        if (quality != null) {
            val nextState = (schedule ?: ReviewState()).next(quality, todayEpochDay, reviewSettings)
            reviews.save(answer.vocabularyItemId, nextState, answeredAtEpochMillis)
        }

        studyRecord.record(
            epochDay = todayEpochDay,
            addedSeconds = studyTimePolicy.creditedSeconds(answeredAtEpochMillis, previousAnswerAtEpochMillis),
            wasNew = !wasReview && answer.outcome.countsAsAnswered,
            wasCorrect = answer.outcome.isCorrect,
        )
    }
}
