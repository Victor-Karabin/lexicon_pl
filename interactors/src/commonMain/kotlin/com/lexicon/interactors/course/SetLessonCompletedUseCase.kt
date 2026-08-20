package com.lexicon.interactors.course

import com.lexicon.model.course.LessonId

interface SetLessonCompletedUseCase {
    suspend operator fun invoke(
        id: LessonId,
        isCompleted: Boolean,
    )
}
