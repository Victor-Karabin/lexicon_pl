package com.lexicon.data.repository

import com.lexicon.boundary.ReviewScheduleRepository
import com.lexicon.data.local.WordReviewDao

class ReviewScheduleRepositoryImpl(
    private val wordReviewDao: WordReviewDao,
) : ReviewScheduleRepository {
    override suspend fun dueWordIds(
        todayEpochDay: Long,
        limit: Int,
    ): List<Long> = wordReviewDao.dueWordIds(todayEpochDay, limit)

    override suspend fun scheduledWordIds(): Set<Long> = wordReviewDao.allScheduledWordIds().toSet()

    override suspend fun countLearned(): Int = wordReviewDao.countLearned()

    override suspend fun countMastered(masteredIntervalDays: Long): Int = wordReviewDao.countMastered(masteredIntervalDays)
}
