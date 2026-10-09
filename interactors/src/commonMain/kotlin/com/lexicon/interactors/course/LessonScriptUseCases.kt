package com.lexicon.interactors.course

import com.lexicon.model.course.LessonId
import com.lexicon.model.course.LessonProgress
import com.lexicon.model.course.LessonScript

fun interface GetLessonScriptUseCase {
    suspend operator fun invoke(id: LessonId): LessonScript?
}

fun interface GetLessonProgressUseCase {
    suspend operator fun invoke(id: LessonId): LessonProgress?
}

fun interface SaveLessonProgressUseCase {
    suspend operator fun invoke(
        id: LessonId,
        progress: LessonProgress,
    )
}
