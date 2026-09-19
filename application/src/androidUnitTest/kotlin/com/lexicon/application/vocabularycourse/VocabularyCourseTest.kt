package com.lexicon.application.vocabularycourse

import com.lexicon.boundary.VocabularyCourseBoundary
import com.lexicon.boundary.VocabularyCourseRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.vocabularycourse.CourseSettings
import com.lexicon.interactors.vocabularycourse.defaultCourseQueue
import com.lexicon.model.training.TrainingType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabularyCourseTest {
    private class InMemoryCourses(initial: VocabularyCourseBoundary? = null) : VocabularyCourseRepository {
        val stored = MutableStateFlow(initial)

        override fun observe(): Flow<VocabularyCourseBoundary?> = stored

        override suspend fun get(): VocabularyCourseBoundary? = stored.value

        override suspend fun save(course: VocabularyCourseBoundary) {
            stored.value = course
        }
    }

    private val learning = (1L..10L).toList()
    private val known = (100L..114L).toList()

    private val vocabulary = mockk<VocabularyRepository> {
        coEvery { learningWordIds(any()) } answers { learning.take(firstArg()) }
        coEvery { randomKnownWordIds(any()) } answers { known.take(firstArg()) }
    }

    private fun course(
        queue: List<String>,
        position: Int = 0,
        round: Int = 0,
        cardsSeenRound: Int = -1,
    ) = VocabularyCourseBoundary(
        newWordsADay = 10,
        reviewsADay = 15,
        queue = queue,
        position = position,
        round = round,
        cardsSeenRound = cardsSeenRound,
    )

    private val threeTrainings = listOf("word_match", "dictation", "true_or_false")

    @Test
    fun `the course exists from the start with ten new words, fifteen reviews and the full queue`() =
        runTest {
            val course = GetVocabularyCourseUseCaseImpl(InMemoryCourses(), vocabulary)()

            assertEquals(10, course.settings.newWordsADay)
            assertEquals(15, course.settings.reviewsADay)
            assertEquals(defaultCourseQueue, course.trainings)
            assertEquals(TrainingType.WORD_CARD, course.nextTraining)
        }

    @Test
    fun `finishing the last training starts the queue again rather than ending the day`() =
        runTest {
            val courses = InMemoryCourses(course(threeTrainings))
            val next = NextCourseTrainingUseCaseImpl(courses, vocabulary)

            assertEquals(TrainingType.DICTATION, next.advance()?.training)
            assertEquals(TrainingType.TRUE_OR_FALSE, next.advance()?.training)
            assertEquals(TrainingType.WORD_MATCH, next.advance()?.training)

            assertEquals(0, courses.stored.value?.position)
            assertEquals(1, courses.stored.value?.round)
        }

    @Test
    fun `a session trains the ten words being learnt and fifteen known ones`() =
        runTest {
            val launch = NextCourseTrainingUseCaseImpl(InMemoryCourses(course(threeTrainings)), vocabulary).next()

            assertEquals((learning + known).toSet(), launch?.wordIds?.map { it.value }?.toSet())
        }

    @Test
    fun `a training that needs more words than there are is stepped over, and the step is kept`() =
        runTest {
            coEvery { vocabulary.learningWordIds(any()) } returns listOf(1L, 2L, 3L)
            coEvery { vocabulary.randomKnownWordIds(any()) } returns emptyList()
            val courses = InMemoryCourses(course(listOf("crossword", "dictation")))

            val launch = NextCourseTrainingUseCaseImpl(courses, vocabulary).next()

            assertEquals(TrainingType.DICTATION, launch?.training)
            assertEquals(1, courses.stored.value?.position)
        }

    @Test
    fun `with nothing marked to learn and nothing known there is nothing to launch`() =
        runTest {
            coEvery { vocabulary.learningWordIds(any()) } returns emptyList()
            coEvery { vocabulary.randomKnownWordIds(any()) } returns emptyList()

            assertNull(NextCourseTrainingUseCaseImpl(InMemoryCourses(course(threeTrainings)), vocabulary).next())
        }

    @Test
    fun `reset goes back to the first training and offers the new words again`() =
        runTest {
            val courses = InMemoryCourses(course(threeTrainings, position = 2, cardsSeenRound = 0))

            ResetCourseQueueUseCaseImpl(courses)()

            val course = GetVocabularyCourseUseCaseImpl(courses, vocabulary)()
            assertEquals(0, course.position)
            assertTrue(course.showCardsNext)
        }

    @Test
    fun `the new-word cards come once a round, before its first training`() =
        runTest {
            val courses = InMemoryCourses(course(threeTrainings))
            val get = GetVocabularyCourseUseCaseImpl(courses, vocabulary)

            assertTrue(get().showCardsNext)
            MarkCourseCardsSeenUseCaseImpl(courses)()
            assertFalse(get().showCardsNext)

            val next = NextCourseTrainingUseCaseImpl(courses, vocabulary)
            repeat(threeTrainings.size) { next.advance() }

            assertTrue(get().showCardsNext)
        }

    @Test
    fun `changing the queue starts it from the top, changing only the amounts keeps the place`() =
        runTest {
            val courses = InMemoryCourses(course(threeTrainings, position = 2))
            val update = UpdateCourseSettingsUseCaseImpl(courses)

            update(CourseSettings(newWordsADay = 5, reviewsADay = 5, queue = persistentListOf(*threeTrainings.toTypedArray())))
            assertEquals(2, courses.stored.value?.position)
            assertEquals(5, courses.stored.value?.newWordsADay)

            update(CourseSettings(newWordsADay = 5, reviewsADay = 5, queue = persistentListOf("dictation")))
            assertEquals(0, courses.stored.value?.position)
            assertEquals(listOf("dictation"), courses.stored.value?.queue)
        }

    @Test
    fun `an emptied queue is not saved, so the course always has something to run`() =
        runTest {
            val courses = InMemoryCourses(course(threeTrainings))

            UpdateCourseSettingsUseCaseImpl(courses)(CourseSettings(queue = persistentListOf()))

            assertEquals(threeTrainings, courses.stored.value?.queue)
        }
}
