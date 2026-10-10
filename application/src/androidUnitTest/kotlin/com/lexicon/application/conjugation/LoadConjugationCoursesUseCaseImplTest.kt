package com.lexicon.application.conjugation

import com.lexicon.boundary.ConjugationProgressBoundary
import com.lexicon.boundary.ConjugationRepository
import com.lexicon.boundary.VerbConjugationBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.conjugation.VERB_COURSE_ID
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadConjugationCoursesUseCaseImplTest {
    private fun regular(infinitive: String) =
        VerbConjugationBoundary(
            infinitive,
            mapOf("ja" to listOf("${infinitive.dropLast(2)}m"), "ty" to listOf("${infinitive.dropLast(2)}sz")),
        )

    private val conjugations: ConjugationRepository = mockk {
        coEvery { verbs() } returns
            listOf(regular("czytać"), regular("mieszkać"), regular("pływać"), VerbConjugationBoundary("być", emptyMap()))
        coEvery { progress(VERB_COURSE_ID) } returns listOf(ConjugationProgressBoundary("czytać", "ja", 2, 2, 0, 2))
    }

    private fun studySet(vararg starred: String): VocabularyRepository =
        mockk {
            coEvery { studySetTextsAmong(any()) } answers { firstArg<List<String>>().filter { it in starred }.toSet() }
        }

    @Test
    fun `the verb course holds every teachable verb marked to learn or favourite`() =
        runTest {
            val courses = LoadConjugationCoursesUseCaseImpl(conjugations, studySet("czytać", "pływać", "być"))()

            val course = courses.single()
            assertEquals(VERB_COURSE_ID, course.id)
            assertEquals(listOf("czytać", "pływać"), course.infinitives)
            assertEquals(4, course.progress.total)
            assertEquals(1, course.progress.mastered)
        }

    @Test
    fun `there is no verb course while no verb is marked`() =
        runTest {
            assertTrue(LoadConjugationCoursesUseCaseImpl(conjugations, studySet())().isEmpty())
        }
}
