package com.lexicon.interactors.course

import com.lexicon.model.course.LessonId
import com.lexicon.model.course.LessonProgress

fun interface SaveLessonResultsUseCase {
    suspend operator fun invoke(
        id: LessonId,
        progress: LessonProgress,
    )
}

fun interface GetLastLessonResultsUseCase {
    suspend operator fun invoke(id: LessonId): LessonResults?
}
