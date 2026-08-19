package com.lexicon.interactors.course

import com.lexicon.model.course.LessonId

interface GetLessonUseCase {
    suspend operator fun invoke(id: LessonId): Lesson?
}
