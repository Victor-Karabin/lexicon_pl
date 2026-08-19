package com.lexicon.data.local

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabularySeederTest {
    private val wordDao: WordDao = mockk(relaxed = true)
    private val vocabularySeedAssetLoader: VocabularySeedAssetLoader = mockk()
    private val vocabularySyncStore: CatalogSeedStore = mockk(relaxed = true)

    private fun seeder() = VocabularySeeder(wordDao, vocabularySeedAssetLoader, vocabularySyncStore)

    private fun word(
        id: Long,
        text: String,
        translation: String = "x",
        cefr: String = "A1",
        isFavourite: Boolean = false,
    ) = WordEntity(
        id = id,
        text = text,
        translation = translation,
        transcription = "",
        isFavourite = isFavourite,
        searchKey = searchKeyFor(text, translation),
        cefr = cefr,
    )

    private fun assetIs(vararg words: WordEntity) {
        coEvery { vocabularySeedAssetLoader.fingerprint() } returns "fp-${words.size}"
        coEvery { vocabularySeedAssetLoader.load() } returns words.toList()
    }

    private fun tableIs(vararg words: WordEntity) {
        coEvery { wordDao.getAll() } returns words.toList()
        coEvery { wordDao.count() } returns words.size
        coEvery { wordDao.countIncludingDeleted() } returns words.size
    }

    @Test
    fun `an empty table is seeded from the asset`() =
        runTest {
            assetIs(word(1, "kot"), word(2, "pies"))
            tableIs()
            coEvery { vocabularySyncStore.syncedFingerprint() } returns null

            seeder().ensureSeeded()

            coVerify { wordDao.insertAll(match { it.size == 2 }) }
        }

    @Test
    fun `words added to the asset reach an already-seeded table`() =
        runTest {
            assetIs(word(1, "kot"), word(2, "pies"), word(3, "dom"))
            tableIs(word(1, "kot"), word(2, "pies"))
            coEvery { vocabularySyncStore.syncedFingerprint() } returns "stale"
            val added = slot<List<WordEntity>>()
            coEvery { wordDao.reconcile(capture(added), any(), any()) } returns Unit

            seeder().ensureSeeded()

            assertEquals(listOf(3L), added.captured.map { it.id })
        }

    @Test
    fun `words dropped from the asset are removed from the table`() =
        runTest {
            assetIs(word(1, "kot"))
            tableIs(word(1, "kot"), word(2, "w"))
            coEvery { vocabularySyncStore.syncedFingerprint() } returns "stale"
            val removedIds = slot<List<Long>>()
            coEvery { wordDao.reconcile(any(), capture(removedIds), any()) } returns Unit

            seeder().ensureSeeded()

            assertEquals(listOf(2L), removedIds.captured)
        }

    @Test
    fun `a deleted word stays deleted when its word is refreshed`() =
        runTest {
            assetIs(word(1, "kot", translation = "cat"))
            tableIs(word(1, "kot", translation = "kitten").copy(isDeleted = true))
            coEvery { vocabularySyncStore.syncedFingerprint() } returns "stale"
            val changed = slot<List<WordEntity>>()
            coEvery { wordDao.reconcile(any(), any(), capture(changed)) } returns Unit

            seeder().ensureSeeded()

            assertTrue("the deletion must not be undone", changed.captured.single().isDeleted)
            assertEquals("the correction is still taken", "cat", changed.captured.single().translation)
        }

    @Test
    fun `a favourite survives its word being refreshed`() =
        runTest {
            assetIs(word(1, "kot", translation = "cat"))
            tableIs(word(1, "kot", translation = "kitten", isFavourite = true))
            coEvery { vocabularySyncStore.syncedFingerprint() } returns "stale"
            val changed = slot<List<WordEntity>>()
            coEvery { wordDao.reconcile(any(), any(), capture(changed)) } returns Unit

            seeder().ensureSeeded()

            val row = changed.captured.single()
            assertEquals("the corrected translation must be taken", "cat", row.translation)
            assertTrue("the heart must not be", row.isFavourite)
        }

    @Test
    fun `rows that already match the asset are not rewritten`() =
        runTest {
            assetIs(word(1, "kot"), word(2, "pies"))
            tableIs(word(1, "kot"), word(2, "pies", isFavourite = true))
            coEvery { vocabularySyncStore.syncedFingerprint() } returns "stale"
            val changed = slot<List<WordEntity>>()
            coEvery { wordDao.reconcile(any(), any(), capture(changed)) } returns Unit

            seeder().ensureSeeded()

            assertTrue(changed.captured.isEmpty())
        }

    @Test
    fun `the diff is applied as a single reconcile call, not separate writes`() =
        runTest {
            assetIs(word(1, "kot"), word(3, "dom"))
            tableIs(word(1, "kot"), word(2, "w"))
            coEvery { vocabularySyncStore.syncedFingerprint() } returns "stale"

            seeder().ensureSeeded()

            coVerify(exactly = 1) { wordDao.reconcile(any(), any(), any()) }
            coVerify(exactly = 0) { wordDao.insertAll(any()) }
            coVerify(exactly = 0) { wordDao.deleteByIds(any()) }
            coVerify(exactly = 0) { wordDao.updateAll(any()) }
        }

    @Test
    fun `an unchanged asset is never parsed`() =
        runTest {
            coEvery { vocabularySeedAssetLoader.fingerprint() } returns "fp-2"
            coEvery { vocabularySyncStore.syncedFingerprint() } returns "fp-2"
            coEvery { wordDao.countIncludingDeleted() } returns 2

            seeder().ensureSeeded()

            coVerify(exactly = 0) { vocabularySeedAssetLoader.load() }
        }

    @Test
    fun `an emptied table is reseeded even when the asset has not changed`() =
        runTest {
            assetIs(word(1, "kot"))
            tableIs()
            coEvery { vocabularySeedAssetLoader.fingerprint() } returns "fp-1"
            coEvery { vocabularySyncStore.syncedFingerprint() } returns "fp-1"

            seeder().ensureSeeded()

            coVerify { wordDao.insertAll(match { it.size == 1 }) }
        }

    @Test
    fun `the sync runs once per process, not once per call`() =
        runTest {
            assetIs(word(1, "kot"))
            tableIs(word(1, "kot"))
            coEvery { vocabularySyncStore.syncedFingerprint() } returns "stale"
            val seeder = seeder()

            seeder.ensureSeeded()
            seeder.ensureSeeded()
            seeder.ensureSeeded()

            coVerify(exactly = 1) { vocabularySeedAssetLoader.load() }
        }

    @Test
    fun `the new fingerprint is recorded once the table matches`() =
        runTest {
            assetIs(word(1, "kot"))
            tableIs(word(1, "kot"))
            coEvery { vocabularySyncStore.syncedFingerprint() } returns "stale"

            seeder().ensureSeeded()

            coVerify { vocabularySyncStore.setSyncedFingerprint("fp-1") }
        }
}
