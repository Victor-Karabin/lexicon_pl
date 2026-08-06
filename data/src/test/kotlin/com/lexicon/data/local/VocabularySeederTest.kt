package com.lexicon.data.local

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class VocabularySeederTest {
    private val wordDao: WordDao = mockk()
    private val vocabularySeedAssetLoader: VocabularySeedAssetLoader = mockk()
    private val seeder = VocabularySeeder(wordDao, vocabularySeedAssetLoader)

    @Test
    fun `seeds from assets when the words table is empty`() =
        runTest {
            coEvery { wordDao.count() } returns 0
            val words = listOf(WordEntity(id = 1, text = "kot", translation = "cat", transcription = "kot"))
            coEvery { vocabularySeedAssetLoader.load() } returns words
            coEvery { wordDao.insertAll(words) } returns Unit

            seeder.ensureSeeded()

            coVerify { wordDao.insertAll(words) }
        }

    @Test
    fun `does nothing when the words table already has data`() =
        runTest {
            coEvery { wordDao.count() } returns 42

            seeder.ensureSeeded()

            coVerify(exactly = 0) { vocabularySeedAssetLoader.load() }
            coVerify(exactly = 0) { wordDao.insertAll(any()) }
        }
}
