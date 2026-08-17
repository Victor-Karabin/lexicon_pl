package com.lexicon.interactors.course

interface SetLessonCompletedUseCase {
    suspend operator fun invoke(
        id: LessonId,
        isCompleted: Boolean,
    )
}
