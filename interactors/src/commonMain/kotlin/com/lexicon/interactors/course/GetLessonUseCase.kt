package com.lexicon.interactors.course

interface GetLessonUseCase {
    suspend operator fun invoke(id: LessonId): Lesson?
}
