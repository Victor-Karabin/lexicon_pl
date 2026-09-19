package com.lexicon.application.vocabularycourse

import com.lexicon.boundary.ImageProvider
import com.lexicon.boundary.StudyRecordRepository
import com.lexicon.boundary.VocabularyCourseBoundary
import com.lexicon.boundary.VocabularyCourseRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.common.Clock
import com.lexicon.interactors.vocabularycourse.CourseLaunch
import com.lexicon.interactors.vocabularycourse.CourseSettings
import com.lexicon.interactors.vocabularycourse.GetCourseProgressUseCase
import com.lexicon.interactors.vocabularycourse.GetStudyStreakUseCase
import com.lexicon.interactors.vocabularycourse.GetVocabularyCourseUseCase
import com.lexicon.interactors.vocabularycourse.GetWordCardsUseCase
import com.lexicon.interactors.vocabularycourse.MarkCourseCardsSeenUseCase
import com.lexicon.interactors.vocabularycourse.NextCourseTrainingUseCase
import com.lexicon.interactors.vocabularycourse.ObserveVocabularyCourseUseCase
import com.lexicon.interactors.vocabularycourse.ResetCourseQueueUseCase
import com.lexicon.interactors.vocabularycourse.UpdateCourseSettingsUseCase
import com.lexicon.interactors.vocabularycourse.VocabularyCourse
import com.lexicon.interactors.vocabularycourse.WordCard
import com.lexicon.model.training.TrainingType
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.WordStatus
import com.lexicon.model.vocabularycourse.CourseProgress
import com.lexicon.model.vocabularycourse.ProgressMetric
import com.lexicon.model.vocabularycourse.ProgressMetricType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest

private const val PERCENT = 100

internal val defaultCourse: VocabularyCourseBoundary
    get() = CourseSettings().let { VocabularyCourseBoundary(it.newWordsADay, it.reviewsADay, it.queue) }

internal suspend fun VocabularyCourseRepository.current(): VocabularyCourseBoundary = get() ?: defaultCourse

private fun VocabularyCourseBoundary.settings(): CourseSettings =
    CourseSettings(
        newWordsADay = newWordsADay,
        reviewsADay = reviewsADay,
        queue = queue,
    )

private suspend fun VocabularyCourseBoundary.toCourse(vocabulary: VocabularyRepository): VocabularyCourse =
    VocabularyCourse(
        settings = settings(),
        position = position.coerceIn(0, queue.size),
        round = round,
        cardsSeen = cardsSeenRound == round,
        newWords = vocabulary.learningWordIds(newWordsADay.coerceAtLeast(0)).map(::VocabularyId).toImmutableList(),
    )

class ObserveVocabularyCourseUseCaseImpl(
    private val courses: VocabularyCourseRepository,
    private val vocabulary: VocabularyRepository,
) : ObserveVocabularyCourseUseCase {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(): Flow<VocabularyCourse> =
        combine(courses.observe(), vocabulary.observeWordStatuses()) { course, _ -> course ?: defaultCourse }
            .mapLatest { it.toCourse(vocabulary) }
            .distinctUntilChanged()
}

class GetVocabularyCourseUseCaseImpl(
    private val courses: VocabularyCourseRepository,
    private val vocabulary: VocabularyRepository,
) : GetVocabularyCourseUseCase {
    override suspend fun invoke(): VocabularyCourse = courses.current().toCourse(vocabulary)
}

class UpdateCourseSettingsUseCaseImpl(
    private val courses: VocabularyCourseRepository,
) : UpdateCourseSettingsUseCase {
    override suspend fun invoke(settings: CourseSettings) {
        val current = courses.current()
        val queue = settings.queue.filter { TrainingType.ofId(it) != null }.ifEmpty { current.queue }
        val queueChanged = queue != current.queue

        courses.save(
            current.copy(
                newWordsADay = settings.newWordsADay.coerceIn(0, CourseSettings.MAX_WORDS_A_DAY),
                reviewsADay = settings.reviewsADay.coerceIn(0, CourseSettings.MAX_WORDS_A_DAY),
                queue = queue,
                position = if (queueChanged) 0 else current.position,
            ),
        )
    }
}

class NextCourseTrainingUseCaseImpl(
    private val courses: VocabularyCourseRepository,
    private val vocabulary: VocabularyRepository,
) : NextCourseTrainingUseCase {
    override suspend fun next(): CourseLaunch? = launchFrom(courses.current())

    override suspend fun advance(): CourseLaunch? {
        val current = courses.current()
        if (current.queue.isEmpty()) return null

        val stepped = current.position + 1
        val advanced = if (stepped >= current.queue.size) {
            current.copy(position = 0, round = current.round + 1)
        } else {
            current.copy(position = stepped)
        }
        courses.save(advanced)
        return launchFrom(advanced)
    }

    private suspend fun launchFrom(course: VocabularyCourseBoundary): CourseLaunch? {
        val words = sessionWords(course).ifEmpty { return null }

        val size = course.queue.size
        for (skipped in 0 until size) {
            val index = (course.position.coerceIn(0, size - 1) + skipped) % size
            val training = TrainingType.ofId(course.queue[index]) ?: continue
            if (words.size < training.minimumWords) continue

            if (index != course.position) courses.save(course.copy(position = index))
            return CourseLaunch(training = training, wordIds = words)
        }
        return null
    }

    private suspend fun sessionWords(course: VocabularyCourseBoundary): ImmutableList<VocabularyId> {
        val learning = vocabulary.learningWordIds(course.newWordsADay.coerceAtLeast(0))
        val known = vocabulary
            .randomKnownWordIds(course.reviewsADay.coerceAtLeast(0))
            .filterNot { it in learning }

        return (learning + known).shuffled().map(::VocabularyId).toImmutableList()
    }
}

class ResetCourseQueueUseCaseImpl(
    private val courses: VocabularyCourseRepository,
) : ResetCourseQueueUseCase {
    override suspend fun invoke() {
        courses.save(courses.current().copy(position = 0, cardsSeenRound = -1))
    }
}

class MarkCourseCardsSeenUseCaseImpl(
    private val courses: VocabularyCourseRepository,
) : MarkCourseCardsSeenUseCase {
    override suspend fun invoke() {
        val current = courses.current()
        courses.save(current.copy(cardsSeenRound = current.round))
    }
}

class GetCourseProgressUseCaseImpl(
    private val vocabulary: VocabularyRepository,
    private val study: StudyRecordRepository,
    private val clock: Clock,
) : GetCourseProgressUseCase {
    override suspend fun invoke(): CourseProgress {
        val known = vocabulary.countWithStatus(WordStatus.KNOWN)
        val learning = vocabulary.countWithStatus(WordStatus.TO_LEARN) + vocabulary.countWithStatus(WordStatus.FAVOURITE)

        val today = study.day(clock.todayEpochDay())
        val answers = today?.answers ?: 0

        return CourseProgress(
            metrics = persistentListOf(
                ProgressMetric(type = ProgressMetricType.VOCABULARY, current = known, target = known + learning),
                ProgressMetric(
                    type = ProgressMetricType.ACCURACY,
                    current = if (today == null || answers == 0) 0 else today.correctAnswers * PERCENT / answers,
                    target = PERCENT,
                    isMeasured = answers > 0,
                ),
            ),
        )
    }
}

class GetStudyStreakUseCaseImpl(
    private val study: StudyRecordRepository,
    private val clock: Clock,
) : GetStudyStreakUseCase {
    override suspend fun invoke(): Int = study.currentStreak(clock.todayEpochDay())
}

class GetWordCardsUseCaseImpl(
    private val vocabulary: VocabularyRepository,
    private val imageProvider: ImageProvider,
) : GetWordCardsUseCase {
    override suspend fun invoke(ids: List<VocabularyId>): ImmutableList<WordCard> {
        if (ids.isEmpty()) return persistentListOf()
        val words = vocabulary.getItemsByIds(ids.map { it.value }).associateBy { it.id.value }

        return ids
            .mapNotNull { words[it.value] }
            .map { word ->
                WordCard(
                    id = word.id,
                    text = word.text,
                    translation = word.translation,
                    transcription = word.transcription,
                    imageUrl = runCatching { imageProvider.searchImage(word.translation) }.getOrNull(),
                    example = word.example,
                )
            }.toImmutableList()
    }
}
