package com.lexicon.application.vocabularycourse

import com.lexicon.boundary.ReviewScheduleRepository
import com.lexicon.boundary.VocabularyCourseBoundary
import com.lexicon.boundary.VocabularyCourseRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.common.Clock
import com.lexicon.interactors.vocabularycourse.CourseSettings
import com.lexicon.interactors.vocabularycourse.defaultCourseQueue
import com.lexicon.model.scheduling.RecallQuality
import com.lexicon.model.scheduling.ReviewState
import com.lexicon.model.scheduling.next
import com.lexicon.model.training.TrainingType
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import io.mockk.coEvery
import io.mockk.coVerify
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

    private class InMemoryReviews : ReviewScheduleRepository {
        val stored = mutableMapOf<Long, ReviewState>()

        override suspend fun find(wordId: Long): ReviewState? = stored[wordId]

        override suspend fun save(
            wordId: Long,
            state: ReviewState,
            reviewedAtEpochMillis: Long,
        ) {
            stored[wordId] = state
        }

        override suspend fun knownWordSchedules(): Map<Long, ReviewState> = stored.toMap()
    }

    private val clock = object : Clock {
        override fun nowEpochMillis(): Long = NOW

        override fun todayEpochDay(): Long = TODAY
    }

    private val learning = (1L..10L).toList()
    private val due = (100L..114L).toList()

    private val vocabulary = mockk<VocabularyRepository> {
        coEvery { learningWordIds(any()) } answers { learning.take(firstArg()) }
        coEvery { unscheduledKnownWordIds(any()) } returns emptyList()
    }
    private val reviews = InMemoryReviews()

    private fun next(courses: InMemoryCourses) =
        NextCourseTrainingUseCaseImpl(courses = courses, vocabulary = vocabulary, reviews = reviews, clock = clock)

    private fun overdueBy(days: Long) = ReviewState(repetitions = 2, intervalDays = 6, dueAtEpochDay = TODAY - days)

    private fun givenDue(ids: Iterable<Long>) = ids.forEach { reviews.stored[it] = overdueBy(days = 1) }

    private fun givenNotYetDue(ids: Iterable<Long>) =
        ids.forEach { reviews.stored[it] = ReviewState(repetitions = 2, intervalDays = 6, dueAtEpochDay = TODAY + 3) }

    private fun givenDueMostOverdueLast(ids: List<Long>) =
        ids.forEachIndexed { index, id -> reviews.stored[id] = overdueBy(days = index + 1L) }

    private suspend fun answer(
        wordId: Long,
        quality: RecallQuality = RecallQuality.PERFECT,
    ) = reviews.save(wordId, checkNotNull(reviews.find(wordId)).next(quality, TODAY), NOW)

    private suspend fun NextCourseTrainingUseCaseImpl.sessionWords(): List<Long> = checkNotNull(next()).wordIds.map { it.value }

    private fun course(
        queue: List<String>,
        position: Int = 0,
        round: Int = 0,
        cardsSeenRound: Int = -1,
        reviewsADay: Int = 15,
    ) = VocabularyCourseBoundary(
        newWordsADay = 10,
        reviewsADay = reviewsADay,
        queue = queue,
        position = position,
        round = round,
        cardsSeenRound = cardsSeenRound,
    )

    private val threeTrainings = listOf("word_match", "dictation", "true_or_false")
    private val oneTraining = listOf("dictation")

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
            val next = next(courses)

            assertEquals(TrainingType.DICTATION, next.advance()?.training)
            assertEquals(TrainingType.TRUE_OR_FALSE, next.advance()?.training)
            assertEquals(TrainingType.WORD_MATCH, next.advance()?.training)

            assertEquals(0, courses.stored.value?.position)
            assertEquals(1, courses.stored.value?.round)
        }

    @Test
    fun `a session trains the ten words being learnt and the fifteen known ones that are due`() =
        runTest {
            givenDue(due)

            val launch = next(InMemoryCourses(course(threeTrainings))).next()

            assertEquals((learning + due).toSet(), launch?.wordIds?.map { it.value }?.toSet())
        }

    @Test
    fun `a session holds the learning words and the due known words, each once`() =
        runTest {
            givenDue(100L..104L)

            val words = next(InMemoryCourses(course(oneTraining))).sessionWords()

            assertEquals(learning + (100L..104L), words.sorted())
        }

    @Test
    fun `more due words than reviews a day leaves the most overdue ones`() =
        runTest {
            givenDueMostOverdueLast((109L downTo 100L).toList())

            val words = next(InMemoryCourses(course(oneTraining, reviewsADay = 5))).sessionWords()

            assertEquals(learning + (100L..104L), words.sorted())
        }

    @Test
    fun `known words that are not due leave a session of only the learning words`() =
        runTest {
            givenNotYetDue(100L..104L)

            val words = next(InMemoryCourses(course(oneTraining))).sessionWords()

            assertEquals(learning, words.sorted())
        }

    @Test
    fun `with nothing to learn and nothing due there is nothing to launch, however many words are known`() =
        runTest {
            coEvery { vocabulary.learningWordIds(any()) } returns emptyList()
            givenNotYetDue(100L..104L)

            assertNull(next(InMemoryCourses(course(oneTraining))).next())
        }

    @Test
    fun `a review answered in one training stays in the course for the trainings after it`() =
        runTest {
            givenDue(listOf(100L, 101L))
            val courses = InMemoryCourses(course(listOf("dictation", "true_or_false", "word_match")))
            val next = next(courses)

            val first = next.next()
            assertTrue(checkNotNull(first).wordIds.map { it.value }.containsAll(listOf(100L, 101L)))

            answer(100L)
            answer(101L, RecallQuality.FORGOTTEN)

            val second = checkNotNull(next.advance()).wordIds.map { it.value }
            assertTrue(second.containsAll(listOf(100L, 101L)))
            val third = checkNotNull(next.advance()).wordIds.map { it.value }
            assertTrue(third.containsAll(listOf(100L, 101L)))
        }

    @Test
    fun `reviewed words count toward the day's cap, so answering does not pull further words in`() =
        runTest {
            givenDueMostOverdueLast((105L downTo 101L).toList())
            val next = next(InMemoryCourses(course(listOf("dictation", "true_or_false"), reviewsADay = 3)))

            val first = next.next()?.wordIds?.map { it.value }.orEmpty()
            assertEquals(learning + (101L..103L), first.sorted())

            (101L..103L).forEach { answer(it) }

            val second = checkNotNull(next.advance()).wordIds.map { it.value }
            assertEquals(learning + (101L..103L), second.sorted())
        }

    @Test
    fun `a word reviewed today holds a slot ahead of words that are overdue`() =
        runTest {
            reviews.stored[100L] = overdueBy(days = 1)
            answer(100L)
            givenDueMostOverdueLast((105L downTo 101L).toList())

            val words = next(InMemoryCourses(course(oneTraining, reviewsADay = 3))).sessionWords()

            assertEquals(learning + listOf(100L, 101L, 102L), words.sorted())
        }

    @Test
    fun `known words without a schedule fill only the slots the due ones leave`() =
        runTest {
            val unscheduled = (200L..210L).toList()
            coEvery { vocabulary.unscheduledKnownWordIds(any()) } answers { unscheduled.take(firstArg()) }
            givenDue(listOf(100L, 101L))

            val words = next(InMemoryCourses(course(oneTraining, reviewsADay = 5))).sessionWords()

            assertEquals(learning + listOf(100L, 101L) + (200L..202L), words.sorted())
            coVerify { vocabulary.unscheduledKnownWordIds(3) }
        }

    @Test
    fun `known words without a schedule are not drawn when the due ones already fill the day`() =
        runTest {
            val unscheduled = (200L..210L).toList()
            coEvery { vocabulary.unscheduledKnownWordIds(any()) } answers { unscheduled.take(firstArg()) }
            givenDue(100L..104L)

            val words = next(InMemoryCourses(course(oneTraining, reviewsADay = 5))).sessionWords()

            assertEquals(learning + (100L..104L), words.sorted())
        }

    @Test
    fun `a learning word that also has a schedule is not listed twice`() =
        runTest {
            givenDue(listOf(1L, 2L, 3L, 100L))

            val words = next(InMemoryCourses(course(oneTraining))).sessionWords()

            assertEquals(words.size, words.toSet().size)
            assertTrue(words.containsAll(learning + 100L))
        }

    @Test
    fun `a training that needs more words than there are is stepped over, and the step is kept`() =
        runTest {
            coEvery { vocabulary.learningWordIds(any()) } returns listOf(1L, 2L, 3L)
            coEvery { vocabulary.unscheduledKnownWordIds(any()) } returns emptyList()
            val courses = InMemoryCourses(course(listOf("crossword", "dictation")))

            val launch = next(courses).next()

            assertEquals(TrainingType.DICTATION, launch?.training)
            assertEquals(1, courses.stored.value?.position)
        }

    @Test
    fun `stepping over the last trainings back to the top starts the next round`() =
        runTest {
            coEvery { vocabulary.learningWordIds(any()) } returns listOf(1L, 2L, 3L)
            coEvery { vocabulary.unscheduledKnownWordIds(any()) } returns emptyList()
            val courses = InMemoryCourses(course(listOf("dictation", "crossword"), position = 1, round = 0, cardsSeenRound = 0))

            val launch = next(courses).next()

            assertEquals(TrainingType.DICTATION, launch?.training)
            assertEquals(0, courses.stored.value?.position)
            assertEquals(1, courses.stored.value?.round)
            assertTrue(GetVocabularyCourseUseCaseImpl(courses, vocabulary)().showCardsNext)
        }

    @Test
    fun `stepping over a training without passing the end keeps the round`() =
        runTest {
            coEvery { vocabulary.learningWordIds(any()) } returns listOf(1L, 2L, 3L)
            coEvery { vocabulary.unscheduledKnownWordIds(any()) } returns emptyList()
            val courses = InMemoryCourses(course(listOf("crossword", "dictation"), position = 0, round = 0, cardsSeenRound = 0))

            next(courses).next()

            assertEquals(1, courses.stored.value?.position)
            assertEquals(0, courses.stored.value?.round)
        }

    @Test
    fun `phrases do not count toward the crossword minimum, so a phrase-heavy session steps over it`() =
        runTest {
            coEvery { vocabulary.learningWordIds(any()) } returns listOf(1L, 2L, 3L, 4L)
            coEvery { vocabulary.unscheduledKnownWordIds(any()) } returns listOf(101L, 102L, 103L, 104L, 105L, 106L)
            coEvery { vocabulary.getItemsByIds(any()) } answers {
                firstArg<List<Long>>().map { id ->
                    Word(VocabularyId(id), if (id % 2 == 0L) "dzień dobry $id" else "slowo$id", "t", "t")
                }
            }
            val courses = InMemoryCourses(course(listOf("crossword", "dictation")))

            val launch = next(courses).next()

            assertEquals(TrainingType.DICTATION, launch?.training)
        }

    @Test
    fun `a crossword is launched when enough single words are in the session`() =
        runTest {
            coEvery { vocabulary.learningWordIds(any()) } returns listOf(1L, 2L, 3L, 4L)
            coEvery { vocabulary.unscheduledKnownWordIds(any()) } returns listOf(101L, 102L, 103L, 104L, 105L, 106L)
            coEvery { vocabulary.getItemsByIds(any()) } answers {
                firstArg<List<Long>>().map { id -> Word(VocabularyId(id), "slowo$id", "t", "t") }
            }
            val courses = InMemoryCourses(course(listOf("crossword", "dictation")))

            val launch = next(courses).next()

            assertEquals(TrainingType.CROSSWORD, launch?.training)
        }

    @Test
    fun `with nothing marked to learn and nothing known there is nothing to launch`() =
        runTest {
            coEvery { vocabulary.learningWordIds(any()) } returns emptyList()
            coEvery { vocabulary.unscheduledKnownWordIds(any()) } returns emptyList()

            assertNull(next(InMemoryCourses(course(threeTrainings))).next())
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

            val next = next(courses)
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

    private companion object {
        const val TODAY = 20_000L
        const val NOW = 1_700_000_000_000L
    }
}
