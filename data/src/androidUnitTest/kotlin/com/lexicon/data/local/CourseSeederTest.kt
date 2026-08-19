package com.lexicon.data.local

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CourseSeederTest {
    private val courseDao: CourseDao = mockk(relaxed = true)
    private val loader: CourseAssetLoader = mockk()
    private val syncStore: CatalogSeedStore = mockk(relaxed = true)

    private fun seeder() = CourseSeeder(courseDao, loader, syncStore)

    private fun lesson(
        number: Int,
        wordIds: List<Long> = listOf(1L, 2L),
    ) = LessonAsset(
        id = "krok-a1-%02d".format(number),
        courseId = "krok-a1",
        number = number,
        title = "Lesson $number",
        vocabularyIds = wordIds,
        audio = listOf(LessonAudioAsset(file = "a1_1${"%02d".format(number)}a1.mp3", section = "A", task = 1)),
    )

    private fun assetIs(vararg lessons: LessonAsset) {
        every { loader.fingerprint() } returns "fp-${lessons.size}"
        every { loader.load() } returns
            CourseCatalogAsset(
                courses = listOf(
                    CourseAsset(
                        id = "krok-a1",
                        order = 1,
                        level = "A1",
                        title = mapOf("en" to "Polski krok po kroku 1"),
                        lessons = lessons.toList(),
                    ),
                ),
            )
    }

    @Test
    fun `an empty database is filled from the asset`() =
        runTest {
            assetIs(lesson(1), lesson(2))
            coEvery { courseDao.countLessons() } returns 0
            coEvery { syncStore.syncedCourseFingerprint() } returns null

            val outcome = seeder().sync()

            assertEquals(2, outcome.total)
            coVerify { courseDao.replaceCatalog(any(), match { it.size == 2 }, any(), any(), any(), any()) }
        }

    @Test
    fun `a changed asset replaces the catalogue`() =
        runTest {
            assetIs(lesson(1), lesson(2), lesson(3))
            coEvery { courseDao.countLessons() } returns 2
            coEvery { syncStore.syncedCourseFingerprint() } returns "stale"
            val lessons = slot<List<LessonEntity>>()
            coEvery { courseDao.replaceCatalog(any(), capture(lessons), any(), any(), any(), any()) } returns Unit

            seeder().sync()

            assertEquals(listOf(1, 2, 3), lessons.captured.map { it.number })
        }

    @Test
    fun `an unchanged asset is not re-imported`() =
        runTest {
            assetIs(lesson(1), lesson(2))
            coEvery { courseDao.countLessons() } returns 2
            coEvery { syncStore.syncedCourseFingerprint() } returns "fp-2"

            val outcome = seeder().sync()

            assertEquals(0, outcome.added)
            coVerify(exactly = 0) { courseDao.replaceCatalog(any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `replacing the catalogue never touches lesson progress`() =
        runTest {
            assetIs(lesson(1), lesson(2))
            coEvery { courseDao.countLessons() } returns 2
            coEvery { syncStore.syncedCourseFingerprint() } returns "stale"

            seeder().sync()

            coVerify(exactly = 0) { courseDao.upsertProgress(any()) }
        }

    @Test
    fun `a lesson keeps its words in the order the book introduces them`() =
        runTest {
            assetIs(lesson(1, wordIds = listOf(30L, 10L, 20L)))
            coEvery { courseDao.countLessons() } returns 0
            coEvery { syncStore.syncedCourseFingerprint() } returns null
            val words = slot<List<LessonWordEntity>>()
            coEvery { courseDao.replaceCatalog(any(), any(), capture(words), any(), any(), any()) } returns Unit

            seeder().sync()

            assertEquals(listOf(30L, 10L, 20L), words.captured.map { it.wordId })
            assertEquals(listOf(0, 1, 2), words.captured.map { it.position })
        }
}
