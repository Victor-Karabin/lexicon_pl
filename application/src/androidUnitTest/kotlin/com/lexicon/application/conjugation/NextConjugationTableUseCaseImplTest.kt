package com.lexicon.application.conjugation

import com.lexicon.boundary.ConjugationProgressBoundary
import com.lexicon.boundary.ConjugationRepository
import com.lexicon.boundary.ImageProvider
import com.lexicon.boundary.VerbConjugationBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.conjugation.VERB_COURSE_ID
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextConjugationTableUseCaseImplTest {
    private fun regular(infinitive: String) =
        VerbConjugationBoundary(
            infinitive,
            mapOf("ja" to listOf("${infinitive.dropLast(2)}m"), "ty" to listOf("${infinitive.dropLast(2)}sz")),
        )

    private val progress = mutableListOf<ConjugationProgressBoundary>()

    private val conjugations: ConjugationRepository = mockk {
        coEvery { verbs() } returns listOf(regular("czytać"), regular("mieszkać"))
        coEvery { progress(VERB_COURSE_ID) } answers { progress.toList() }
    }

    private val vocabulary: VocabularyRepository = mockk(relaxed = true) {
        coEvery { studySetTextsAmong(any()) } answers { firstArg<List<String>>().toSet() }
        coEvery { findWordByText(any()) } returns null
    }

    private val images: ImageProvider = mockk(relaxed = true) {
        coEvery { searchImage(any()) } returns null
    }

    private val next = NextConjugationTableUseCaseImpl(conjugations, vocabulary, images)

    private fun practised(
        infinitive: String,
        attempted: Int,
        streak: Int = 0,
    ) {
        listOf("ja", "ty").forEach { person ->
            progress += ConjugationProgressBoundary(infinitive, person, attempted, streak, attempted - streak, streak)
        }
    }

    @Test
    fun `a verb already asked this session waits while another can be asked`() =
        runTest {
            practised("mieszkać", attempted = 5)

            assertEquals("mieszkać", next(VERB_COURSE_ID, asked = setOf("czytać"))!!.infinitive)
        }

    @Test
    fun `the least practised verb comes first when nothing has been asked`() =
        runTest {
            practised("mieszkać", attempted = 5)

            assertEquals("czytać", next(VERB_COURSE_ID)!!.infinitive)
        }

    @Test
    fun `once every verb has been asked, the session goes on with the least practised`() =
        runTest {
            practised("mieszkać", attempted = 5)

            assertEquals("czytać", next(VERB_COURSE_ID, asked = setOf("czytać", "mieszkać"))!!.infinitive)
        }

    @Test
    fun `a mastered verb is never asked, even when the others were`() =
        runTest {
            practised("mieszkać", attempted = 9, streak = 9)

            assertEquals("czytać", next(VERB_COURSE_ID, asked = setOf("czytać"))!!.infinitive)

            practised("czytać", attempted = 9, streak = 9)
            assertNull(next(VERB_COURSE_ID))
        }
}
