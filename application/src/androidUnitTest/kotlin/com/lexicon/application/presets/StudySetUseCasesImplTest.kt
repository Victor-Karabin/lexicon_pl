package com.lexicon.application.presets

import com.lexicon.boundary.VocabularyRepository
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.WordStatus
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StudySetUseCasesImplTest {
    private val vocabularyRepository: VocabularyRepository = mockk(relaxed = true)

    @Test
    fun `setting a status writes only that word`() =
        runTest {
            SetWordStatusUseCaseImpl(vocabularyRepository)(VocabularyId(7L), WordStatus.FAVOURITE)

            coVerify { vocabularyRepository.setStatus(listOf(7L), WordStatus.FAVOURITE) }
        }

    @Test
    fun `clearing a word writes the undefined status rather than deleting anything`() =
        runTest {
            SetWordStatusUseCaseImpl(vocabularyRepository)(VocabularyId(7L), WordStatus.UNDEFINED)

            coVerify { vocabularyRepository.setStatus(listOf(7L), WordStatus.UNDEFINED) }
        }

    @Test
    fun `the statuses on file reach the screen keyed by word`() =
        runTest {
            every { vocabularyRepository.observeWordStatuses() } returns
                flowOf(mapOf(1L to WordStatus.TO_LEARN, 2L to WordStatus.KNOWN))

            val statuses = ObserveWordStatusesUseCaseImpl(vocabularyRepository)().first()

            assertEquals(
                mapOf(VocabularyId(1L) to WordStatus.TO_LEARN, VocabularyId(2L) to WordStatus.KNOWN),
                statuses,
            )
        }
}
