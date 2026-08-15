package com.lexicon.data.repository

import com.lexicon.boundary.ReviewScheduleRepository
import com.lexicon.boundary.WordScheduleBoundary
import com.lexicon.data.local.WordReviewDao

class ReviewScheduleRepositoryImpl(
    private val wordReviewDao: WordReviewDao,
) : ReviewScheduleRepository {
    override suspend fun dueWordIds(
        todayEpochDay: Long,
        limit: Int,
    ): List<Long> = wordReviewDao.dueWordIds(todayEpochDay, limit)

    override suspend fun schedule(wordId: Long): WordScheduleBoundary? =
        wordReviewDao.find(wordId)?.let {
            WordScheduleBoundary(
                wordId = it.wordId,
                repetitions = it.repetitions,
                easeFactor = it.easeFactor,
                intervalDays = it.intervalDays,
                dueAtEpochDay = it.dueAtEpochDay,
                lapses = it.lapses,
            )
        }

    override suspend fun scheduledWordIds(): Set<Long> = wordReviewDao.allScheduledWordIds().toSet()

    override suspend fun countLearned(): Int = wordReviewDao.countLearned()

    override suspend fun countMastered(masteredIntervalDays: Long): Int = wordReviewDao.countMastered(masteredIntervalDays)
}
