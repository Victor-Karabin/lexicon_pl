package com.lexicon.data.local

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
    fun `does not reseed when the words table already has data`() =
        runTest {
            coEvery { wordDao.count() } returns 42
            coEvery { wordDao.getWithoutSearchKey() } returns emptyList()

            seeder.ensureSeeded()

            coVerify(exactly = 0) { vocabularySeedAssetLoader.load() }
            coVerify(exactly = 0) { wordDao.insertAll(any()) }
        }

    /**
     * Rows carried across the search-key migration arrive with an empty key. Backfilling them
     * here rather than reseeding is what keeps the user's favourites, which live on the same rows.
     */
    @Test
    fun `backfills search keys for rows that predate the column`() =
        runTest {
            coEvery { wordDao.count() } returns 2
            val stale = listOf(
                WordEntity(id = 1, text = "żółw", translation = "turtle", transcription = "ʐuwf"),
                WordEntity(id = 2, text = "kot", translation = "cat", transcription = "kɔt"),
            )
            coEvery { wordDao.getWithoutSearchKey() } returns stale
            val updated = slot<List<WordEntity>>()
            coEvery { wordDao.updateAll(capture(updated)) } returns Unit

            seeder.ensureSeeded()

            assertEquals(listOf("zolw turtle", "kot cat"), updated.captured.map { it.searchKey })
            coVerify(exactly = 0) { vocabularySeedAssetLoader.load() }
        }

    @Test
    fun `leaves rows alone when every search key is already present`() =
        runTest {
            coEvery { wordDao.count() } returns 42
            coEvery { wordDao.getWithoutSearchKey() } returns emptyList()

            seeder.ensureSeeded()

            coVerify(exactly = 0) { wordDao.updateAll(any()) }
        }
}
