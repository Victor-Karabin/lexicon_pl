package com.lexicon.interactors.course

import com.lexicon.model.course.LessonId

fun interface ResetLessonProgressUseCase {
    suspend operator fun invoke(id: LessonId)
}
