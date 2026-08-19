package com.lexicon.domain.crossword

import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.crossword.StartCrosswordSessionRequest
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartCrosswordSessionUseCaseImplTest {
    private val vocabularyRepository: VocabularyRepository = mockk()
    private val useCase = StartCrosswordSessionUseCaseImpl(vocabularyRepository)

    private val items = listOf(
        Word(VocabularyId(1), "kot", "cat", "kɔt"),
        Word(VocabularyId(2), "tor", "track", "tɔr"),
        Word(VocabularyId(3), "dom", "house", "dɔm"),
        Word(VocabularyId(4), "dzień dobry", "good morning", "d͡ʑɛɲ ˈdɔbrɨ"),
        Word(VocabularyId(5), "gdzie jest dworzec?", "where is the station?", "ɡd͡ʑɛ jɛst ˈdvɔʐɛt͡s"),
    )

    @Test
    fun `phrases are excluded from the puzzle`() =
        runTest {
            coEvery { vocabularyRepository.getRandomItems(any()) } returns items

            val response = useCase(StartCrosswordSessionRequest(wordCount = 5))

            assertTrue(response.words.none { it.expectedText.contains(' ') })
            assertTrue(response.words.isNotEmpty())
            assertTrue(response.words.map { it.vocabularyItemId }.all { it in setOf(1L, 2L, 3L) })
        }

    @Test
    fun `no more than wordCount words are placed`() =
        runTest {
            coEvery { vocabularyRepository.getRandomItems(any()) } returns items

            val response = useCase(StartCrosswordSessionRequest(wordCount = 2))

            assertEquals(2, response.words.size)
        }

    @Test
    fun `each clue is the base-language translation of the word to spell`() =
        runTest {
            coEvery { vocabularyRepository.getRandomItems(any()) } returns items

            val response = useCase(StartCrosswordSessionRequest(wordCount = 3))

            val expectedClues = items.filterNot { it.text.contains(' ') }.associate { it.text to it.translation }
            response.words.forEach { placement ->
                assertEquals(expectedClues[placement.expectedText], placement.clueText)
            }
        }
}
