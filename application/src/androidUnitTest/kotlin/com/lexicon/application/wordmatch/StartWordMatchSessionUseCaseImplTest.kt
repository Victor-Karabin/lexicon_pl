package com.lexicon.application.wordmatch

import com.lexicon.application.settings.StepCountResolver
import com.lexicon.boundary.AppSettingsBoundary
import com.lexicon.boundary.SettingsRepository
import com.lexicon.boundary.ThemeModeBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.wordmatch.StartWordMatchSessionRequest
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartWordMatchSessionUseCaseImplTest {
    private val vocabularyRepository: VocabularyRepository = mockk()
    private val settingsRepository: SettingsRepository = mockk {
        coEvery { getSettings() } returns AppSettingsBoundary(ThemeModeBoundary.SYSTEM, stepCount = 10)
    }
    private val useCase = StartWordMatchSessionUseCaseImpl(vocabularyRepository, StepCountResolver(settingsRepository))

    private val singleWords = (1L..10L).map { Word(VocabularyId(it), "słowo$it", "word$it", "ˈswɔvɔ") }
    private val phrases = (11L..20L).map { Word(VocabularyId(it), "dzień dobry $it", "good morning $it", "d͡ʑɛɲ") }

    private fun pairsAreOneContentType(words: List<String>) = words.all { it.contains(' ') } || words.none { it.contains(' ') }

    @Test
    fun `a board never mixes single words with phrases`() =
        runTest {
            coEvery { vocabularyRepository.getRandomItems(any()) } returns (singleWords + phrases).shuffled()

            val pairs = useCase(StartWordMatchSessionRequest(stepCount = 4)).steps.single().pairs

            assertTrue("board mixed content types: ${pairs.map { it.word }}", pairsAreOneContentType(pairs.map { it.word }))
        }

    @Test
    fun `the board is filled to the requested pair count`() =
        runTest {
            coEvery { vocabularyRepository.getRandomItems(any()) } returns (singleWords + phrases).shuffled()

            assertEquals(4, useCase(StartWordMatchSessionRequest(stepCount = 4)).steps.single().pairs.size)
        }

    @Test
    fun `prefers whichever content type can fill the board`() =
        runTest {
            coEvery { vocabularyRepository.getRandomItems(any()) } returns singleWords + phrases.take(2)

            val pairs = useCase(StartWordMatchSessionRequest(stepCount = 4)).steps.single().pairs

            assertEquals(4, pairs.size)
            assertTrue(pairs.none { it.word.contains(' ') })
        }

    @Test
    fun `a phrase-only vocabulary still produces a phrase board`() =
        runTest {
            coEvery { vocabularyRepository.getRandomItems(any()) } returns phrases

            val pairs = useCase(StartWordMatchSessionRequest(stepCount = 4)).steps.single().pairs

            assertEquals(4, pairs.size)
            assertTrue(pairs.all { it.word.contains(' ') })
        }

    @Test
    fun `word and translation stay paired on the same vocabulary item`() =
        runTest {
            coEvery { vocabularyRepository.getRandomItems(any()) } returns singleWords

            val pairs = useCase(StartWordMatchSessionRequest(stepCount = 3)).steps.single().pairs

            pairs.forEach { pair ->
                val source = singleWords.first { it.id.value == pair.vocabularyItemId }
                assertEquals(source.text, pair.word)
                assertEquals(source.translation, pair.translation)
            }
        }
}
