package com.lexicon.domain.course

import com.lexicon.boundary.CourseRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.course.GetLessonUseCase
import com.lexicon.interactors.course.GetLessonVocabularyUseCase
import com.lexicon.interactors.course.Lesson
import com.lexicon.interactors.course.ObserveCoursesUseCase
import com.lexicon.interactors.course.SetLessonCompletedUseCase
import com.lexicon.model.course.Course
import com.lexicon.model.course.LessonId
import com.lexicon.model.vocabulary.Word
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveCoursesUseCaseImpl(
    private val repository: CourseRepository,
) : ObserveCoursesUseCase {
    override fun invoke(): Flow<ImmutableList<Course>> =
        repository.observeCourses().map { courses ->
            courses.map { it.toCourse() }.sortedBy { it.order }.toImmutableList()
        }
}

class GetLessonUseCaseImpl(
    private val repository: CourseRepository,
) : GetLessonUseCase {
    override suspend fun invoke(id: LessonId): Lesson? = repository.getLesson(id.value)?.toLesson()
}

class GetLessonVocabularyUseCaseImpl(
    private val courseRepository: CourseRepository,
    private val vocabularyRepository: VocabularyRepository,
) : GetLessonVocabularyUseCase {
    override suspend fun invoke(id: LessonId): ImmutableList<Word> {
        val wordIds = courseRepository.getLessonWordIds(id.value)
        if (wordIds.isEmpty()) return persistentListOf()
        val byId = vocabularyRepository.getItemsByIds(wordIds).associateBy { it.id.value }
        return wordIds.mapNotNull { byId[it] }.toImmutableList()
    }
}

class SetLessonCompletedUseCaseImpl(
    private val repository: CourseRepository,
) : SetLessonCompletedUseCase {
    override suspend fun invoke(
        id: LessonId,
        isCompleted: Boolean,
    ) = repository.setLessonCompleted(id.value, isCompleted)
}
