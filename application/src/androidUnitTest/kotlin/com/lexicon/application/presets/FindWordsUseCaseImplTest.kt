package com.lexicon.application.presets

import com.lexicon.boundary.VocabularyRepository
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FindWordsUseCaseImplTest {
    private fun word(
        id: Long,
        text: String,
    ) = Word(VocabularyId(id), text, "gloss", "")

    private val stored = listOf(
        word(1, "część"),
        word(2, "cześć"),
        word(3, "jak się mówi po polsku?"),
        word(4, "co"),
        word(5, "Czy wiesz…?"),
    )

    private val repository: VocabularyRepository = mockk {
        coEvery { wordsWithTexts(any()) } answers { stored.filter { it.text in firstArg<List<String>>() } }
    }

    @Test
    fun `a lesson word finds its own entry by exact spelling, ignoring case and closing punctuation`() =
        runTest {
            val found = FindWordsUseCaseImpl(repository)(listOf("Cześć", "jak się mówi po polsku…?", "Co?", "palma"))

            assertEquals(VocabularyId(2), found.getValue("Cześć").id)
            assertEquals(VocabularyId(3), found.getValue("jak się mówi po polsku…?").id)
            assertEquals(VocabularyId(4), found.getValue("Co?").id)
            assertEquals(VocabularyId(5), FindWordsUseCaseImpl(repository)(listOf("Czy wiesz…?")).getValue("Czy wiesz…?").id)
            assertEquals(setOf("Cześć", "jak się mówi po polsku…?", "Co?"), found.keys)
        }

    @Test
    fun `all lesson words are looked up in one go`() =
        runTest {
            FindWordsUseCaseImpl(repository)(listOf("Cześć", "co", "palma"))

            coVerify(exactly = 1) { repository.wordsWithTexts(match { "cześć" in it && "co" in it && "palma" in it }) }
        }

    @Test
    fun `blank texts match nothing`() =
        runTest {
            assertTrue(FindWordsUseCaseImpl(repository)(listOf(" ", "…")).isEmpty())
        }
}
