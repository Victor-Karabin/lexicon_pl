package com.lexicon.interactors.course

import com.lexicon.interactors.presets.PresetWord
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

interface ObserveCoursesUseCase {
    operator fun invoke(): Flow<ImmutableList<Course>>
}

interface GetLessonUseCase {
    suspend operator fun invoke(id: LessonId): Lesson?
}

interface GetLessonVocabularyUseCase {
    suspend operator fun invoke(id: LessonId): ImmutableList<PresetWord>
}

interface SetLessonCompletedUseCase {
    suspend operator fun invoke(
        id: LessonId,
        isCompleted: Boolean,
    )
}
