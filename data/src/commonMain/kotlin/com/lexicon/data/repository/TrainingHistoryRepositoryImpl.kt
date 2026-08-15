package com.lexicon.data.repository

import com.lexicon.boundary.AccuracyBoundary
import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.boundary.TrainingResultOutcomeBoundary
import com.lexicon.common.Clock
import com.lexicon.common.RecallQuality
import com.lexicon.common.ReviewSettings
import com.lexicon.common.ReviewState
import com.lexicon.common.next
import com.lexicon.data.local.StudyDayDao
import com.lexicon.data.local.TrainingResultDao
import com.lexicon.data.local.TrainingResultEntity
import com.lexicon.data.local.WordReviewDao
import com.lexicon.data.local.toEntity
import com.lexicon.data.local.toState

/**
 * The longest a pause between two answers still counts as studying.
 *
 * Study time is the gap between consecutive answers rather than anything the
 * training screens report, which keeps all ten of them out of it. A learner who puts
 * the phone down mid-session and comes back an hour later should not be credited with
 * the hour, so anything past this is treated as having left and returned.
 */
private const val MAX_GAP_BETWEEN_ANSWERS_SECONDS = 120L

private const val MILLIS_PER_SECOND = 1000L

class TrainingHistoryRepositoryImpl(
    private val trainingResultDao: TrainingResultDao,
    private val wordReviewDao: WordReviewDao,
    private val studyDayDao: StudyDayDao,
    private val clock: Clock,
) : TrainingHistoryRepository {
    /**
     * Records the answer, moves the word's schedule on, and folds the answer into
     * today's tally — one call, because they are one event.
     */
    override suspend fun recordResult(result: TrainingResultBoundary) {
        val existing = wordReviewDao.find(result.vocabularyItemId)
        val wasReview = existing != null
        // Read before the insert, or the "previous" answer is this one and every gap
        // measures zero.
        val previousAnswerAtEpochMillis = trainingResultDao.lastAnsweredAtEpochMillis()

        trainingResultDao.insert(
            TrainingResultEntity(
                sessionId = result.sessionId,
                trainingType = result.trainingType,
                stepIndex = result.stepIndex,
                vocabularyItemId = result.vocabularyItemId,
                expectedAnswer = result.expectedAnswer,
                submittedAnswer = result.submittedAnswer,
                outcome = result.outcome.name,
                tipUsed = result.tipUsed,
                completedAtEpochMillis = result.completedAtEpochMillis,
                wasReview = wasReview,
            ),
        )

        val todayEpochDay = clock.todayEpochDay()
        // A word merely shown moves no schedule: the interval a word earns is earned
        // by answering, and advancing it here would make a glance look like recall.
        val seen = result.outcome == TrainingResultOutcomeBoundary.SEEN
        if (!seen) {
            val nextState = (existing?.toState() ?: ReviewState())
                .next(result.recallQuality(), todayEpochDay, ReviewSettings())
            wordReviewDao.upsert(nextState.toEntity(result.vocabularyItemId, result.completedAtEpochMillis))
        }

        studyDayDao.record(
            epochDay = todayEpochDay,
            addedSeconds = secondsSincePreviousAnswer(result.completedAtEpochMillis, previousAnswerAtEpochMillis),
            wasNew = !wasReview && !seen,
            wasCorrect = result.outcome == TrainingResultOutcomeBoundary.CORRECT,
        )
    }

    /**
     * How long this answer took, taken as the gap since the previous one.
     *
     * The very first answer ever recorded costs nothing — there is no earlier moment
     * to measure from — and every answer after it costs the time since the one before,
     * capped so a long pause is not counted as study.
     */
    private fun secondsSincePreviousAnswer(
        nowEpochMillis: Long,
        previousAnswerAtEpochMillis: Long?,
    ): Long {
        val previous = previousAnswerAtEpochMillis ?: return 0
        val gapSeconds = (nowEpochMillis - previous) / MILLIS_PER_SECOND
        return gapSeconds.coerceIn(0, MAX_GAP_BETWEEN_ANSWERS_SECONDS)
    }

    override suspend fun accuracyBetween(
        fromEpochMillis: Long,
        toEpochMillis: Long,
    ): AccuracyBoundary =
        AccuracyBoundary(
            // Shown-only steps are not answers, so they belong in neither half of it.
            answers = trainingResultDao.countAnswersBetween(SEEN, fromEpochMillis, toEpochMillis),
            correct = trainingResultDao.countCorrectBetween(CORRECT, fromEpochMillis, toEpochMillis),
        )

    override suspend fun countSessionsBetween(
        fromEpochMillis: Long,
        toEpochMillis: Long,
    ): Int = trainingResultDao.countSessionsBetween(fromEpochMillis, toEpochMillis)

    override suspend fun countSessionsOfTrainingBetween(
        trainingType: String,
        fromEpochMillis: Long,
        toEpochMillis: Long,
    ): Int = trainingResultDao.countSessionsOfTrainingBetween(trainingType, fromEpochMillis, toEpochMillis)

    override suspend fun resultsForWord(wordId: Long): List<TrainingResultBoundary> =
        trainingResultDao.forWord(wordId).map { it.toBoundary() }

    private companion object {
        val CORRECT = TrainingResultOutcomeBoundary.CORRECT.name
        val SEEN = TrainingResultOutcomeBoundary.SEEN.name
    }
}

/**
 * The answer as the scheduler reads it. A tip means the word was reached for and not
 * found on its own, which is worth less than recalling it unaided.
 */
private fun TrainingResultBoundary.recallQuality(): RecallQuality =
    when (outcome) {
        TrainingResultOutcomeBoundary.CORRECT -> if (tipUsed) RecallQuality.HESITANT else RecallQuality.PERFECT
        TrainingResultOutcomeBoundary.SKIPPED -> RecallQuality.SKIPPED
        TrainingResultOutcomeBoundary.INCORRECT -> RecallQuality.FORGOTTEN
        // Never reached: a seen word skips the schedule entirely.
        TrainingResultOutcomeBoundary.SEEN -> RecallQuality.SKIPPED
    }

private fun TrainingResultEntity.toBoundary(): TrainingResultBoundary =
    TrainingResultBoundary(
        sessionId = sessionId,
        trainingType = trainingType,
        stepIndex = stepIndex,
        vocabularyItemId = vocabularyItemId,
        expectedAnswer = expectedAnswer,
        submittedAnswer = submittedAnswer,
        outcome = TrainingResultOutcomeBoundary.valueOf(outcome),
        tipUsed = tipUsed,
        completedAtEpochMillis = completedAtEpochMillis,
    )
