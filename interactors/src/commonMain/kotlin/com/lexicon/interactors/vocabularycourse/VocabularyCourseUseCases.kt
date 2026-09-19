package com.lexicon.interactors.vocabularycourse

import com.lexicon.model.vocabularycourse.CourseProgress
import kotlinx.coroutines.flow.Flow

interface ObserveVocabularyCourseUseCase {
    operator fun invoke(): Flow<VocabularyCourse>
}

interface GetVocabularyCourseUseCase {
    suspend operator fun invoke(): VocabularyCourse
}

interface UpdateCourseSettingsUseCase {
    suspend operator fun invoke(settings: CourseSettings)
}

interface NextCourseTrainingUseCase {
    suspend fun next(): CourseLaunch?

    suspend fun advance(): CourseLaunch?
}

interface ResetCourseQueueUseCase {
    suspend operator fun invoke()
}

interface MarkCourseCardsSeenUseCase {
    suspend operator fun invoke()
}

interface GetCourseProgressUseCase {
    suspend operator fun invoke(): CourseProgress
}
