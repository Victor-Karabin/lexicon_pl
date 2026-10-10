package com.lexicon.application.course

import com.lexicon.boundary.CourseRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.boundary.WritingReviewRequestBoundary
import com.lexicon.boundary.WritingReviewResultBoundary
import com.lexicon.boundary.WritingReviewer
import com.lexicon.boundary.WrittenAnswerBoundary
import com.lexicon.interactors.course.GetLessonProgressUseCase
import com.lexicon.interactors.course.GetLessonScriptUseCase
import com.lexicon.interactors.course.GetLessonUseCase
import com.lexicon.interactors.course.GetLessonVocabularyUseCase
import com.lexicon.interactors.course.Lesson
import com.lexicon.interactors.course.ObserveCoursesUseCase
import com.lexicon.interactors.course.ReviewWritingUseCase
import com.lexicon.interactors.course.SaveLessonProgressUseCase
import com.lexicon.interactors.course.SetLessonCompletedUseCase
import com.lexicon.interactors.course.WritingReviewOutcome
import com.lexicon.model.course.Course
import com.lexicon.model.course.LessonId
import com.lexicon.model.course.LessonProgress
import com.lexicon.model.course.LessonScreen
import com.lexicon.model.course.LessonScript
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

class GetLessonScriptUseCaseImpl(
    private val repository: CourseRepository,
) : GetLessonScriptUseCase {
    override suspend fun invoke(id: LessonId): LessonScript? = repository.getLessonScript(id.value)
}

class GetLessonProgressUseCaseImpl(
    private val repository: CourseRepository,
) : GetLessonProgressUseCase {
    override suspend fun invoke(id: LessonId): LessonProgress? = repository.getLessonScriptProgress(id.value)
}

class SaveLessonProgressUseCaseImpl(
    private val repository: CourseRepository,
) : SaveLessonProgressUseCase {
    override suspend fun invoke(
        id: LessonId,
        progress: LessonProgress,
    ) = repository.saveLessonScriptProgress(id.value, progress)
}

class SetLessonCompletedUseCaseImpl(
    private val repository: CourseRepository,
) : SetLessonCompletedUseCase {
    override suspend fun invoke(
        id: LessonId,
        isCompleted: Boolean,
    ) = repository.setLessonCompleted(id.value, isCompleted)
}

class ReviewWritingUseCaseImpl(
    private val reviewer: WritingReviewer,
) : ReviewWritingUseCase {
    override suspend fun invoke(
        screen: LessonScreen.FreeWriting,
        answers: List<String>,
    ): WritingReviewOutcome {
        val request = WritingReviewRequestBoundary(
            task = screen.instruction,
            answers = screen.fields.mapIndexed { index, label -> WrittenAnswerBoundary(label, answers.getOrElse(index) { "" }) },
            model = screen.model,
            criteria = screen.checklist,
        )
        return when (val result = reviewer.review(request)) {
            is WritingReviewResultBoundary.Reviewed -> WritingReviewOutcome.Reviewed(result.review)
            WritingReviewResultBoundary.Offline -> WritingReviewOutcome.Offline
            is WritingReviewResultBoundary.Refused -> WritingReviewOutcome.Unavailable
        }
    }
}
