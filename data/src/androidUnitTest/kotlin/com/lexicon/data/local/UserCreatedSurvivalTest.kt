package com.lexicon.data.local

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Words and presets the learner writes exist only in the database — no asset can put
 * them back. Both seeders reset their tables from the shipped asset, so without the
 * carve-outs these cover, the next app update that changed vocabulary_pl.json or
 * vocabulary_presets.json would quietly delete everything the learner had added.
 */
class UserCreatedSurvivalTest {
    private val presetDao: PresetDao = mockk(relaxed = true)

    private fun preset(
        id: String,
        isUserCreated: Boolean = false,
    ) = PresetEntity(
        id = id,
        categoryId = if (isUserCreated) USER_PRESET_CATEGORY_ID else "cat",
        titleJson = "{}",
        descriptionJson = "{}",
        icon = null,
        color = null,
        popularity = 0,
        estimatedSeconds = 0,
        isUserCreated = isUserCreated,
    )

    @Test
    fun `a preset the learner made is written back after the catalogue is re-seeded`() =
        runTest {
            val mine = preset("my-kitchen", isUserCreated = true)
            coEvery { presetDao.getUserPresets() } returns listOf(mine)
            coEvery { presetDao.getMembershipsOf("my-kitchen") } returns
                listOf(PresetWordEntity("my-kitchen", 5L, position = 0))
            coEvery { presetDao.getOverrides() } returns emptyList()
            coEvery { presetDao.replaceCatalog(any(), any(), any()) } answers { callOriginal() }

            presetDao.replaceCatalog(
                categories = emptyList(),
                presets = listOf(preset("food")),
                memberships = emptyList(),
            )

            val presets = mutableListOf<List<PresetEntity>>()
            coVerify { presetDao.insertPresets(capture(presets)) }
            assertTrue("the learner's preset was dropped by the re-seed", presets.any { mine in it })

            val memberships = mutableListOf<List<PresetWordEntity>>()
            coVerify { presetDao.insertMemberships(capture(memberships)) }
            assertTrue(
                "the words in the learner's preset were dropped by the re-seed",
                memberships.any { PresetWordEntity("my-kitchen", 5L, position = 0) in it },
            )
        }

    @Test
    fun `the category holding the learner's presets is put back too`() =
        runTest {
            coEvery { presetDao.getUserPresets() } returns listOf(preset("my-kitchen", isUserCreated = true))
            coEvery { presetDao.getMembershipsOf(any()) } returns emptyList()
            coEvery { presetDao.getOverrides() } returns emptyList()
            coEvery { presetDao.replaceCatalog(any(), any(), any()) } answers { callOriginal() }

            presetDao.replaceCatalog(emptyList(), listOf(preset("food")), emptyList())

            val categories = mutableListOf<List<PresetCategoryEntity>>()
            coVerify { presetDao.insertCategories(capture(categories)) }
            assertTrue(
                "the my-presets category went missing, leaving the presets unsorted",
                categories.any { batch -> batch.any { it.id == USER_PRESET_CATEGORY_ID } },
            )
        }

    @Test
    fun `nothing extra is written when the learner has made no presets`() =
        runTest {
            coEvery { presetDao.getUserPresets() } returns emptyList()
            coEvery { presetDao.getOverrides() } returns emptyList()
            coEvery { presetDao.replaceCatalog(any(), any(), any()) } answers { callOriginal() }

            presetDao.replaceCatalog(emptyList(), listOf(preset("food")), emptyList())

            val categories = mutableListOf<List<PresetCategoryEntity>>()
            coVerify { presetDao.insertCategories(capture(categories)) }
            assertTrue(
                "an empty my-presets category was created for a learner who has none",
                categories.none { batch -> batch.any { it.id == USER_PRESET_CATEGORY_ID } },
            )
        }

    @Test
    fun `an override naming the learner's own preset is still replayed`() =
        runTest {
            coEvery { presetDao.getUserPresets() } returns listOf(preset("my-kitchen", isUserCreated = true))
            coEvery { presetDao.getMembershipsOf(any()) } returns emptyList()
            coEvery { presetDao.getOverrides() } returns
                listOf(PresetWordOverrideEntity("my-kitchen", 9L, isMember = true))
            coEvery { presetDao.nextPosition("my-kitchen") } returns 3
            coEvery { presetDao.replaceCatalog(any(), any(), any()) } answers { callOriginal() }

            presetDao.replaceCatalog(emptyList(), listOf(preset("food")), emptyList())

            val inserted = slot<PresetWordEntity>()
            coVerify { presetDao.insertMembership(capture(inserted)) }
            assertEquals(PresetWordEntity("my-kitchen", 9L, position = 3), inserted.captured)
        }

    @Test
    fun `a word the learner wrote is not treated as one the asset dropped`() =
        runTest {
            val wordDao: WordDao = mockk(relaxed = true)
            val loader: VocabularySeedAssetLoader = mockk()
            val syncStore: VocabularySyncStore = mockk(relaxed = true)

            val shipped = WordEntity(id = 1, text = "woda", translation = "water", transcription = "ˈvɔda")
            val mine = WordEntity(
                id = -1,
                text = "smok",
                translation = "dragon",
                transcription = "",
                isUserCreated = true,
            )

            coEvery { loader.fingerprint() } returns "new"
            coEvery { syncStore.syncedFingerprint() } returns "old"
            coEvery { loader.load() } returns listOf(shipped)
            coEvery { wordDao.getAll() } returns listOf(shipped, mine)
            coEvery { wordDao.countIncludingDeleted() } returns 2

            VocabularySeeder(wordDao, loader, syncStore).sync()

            val removed = slot<List<Long>>()
            coVerify { wordDao.reconcile(any(), capture(removed), any()) }
            assertFalse("the learner's own word was deleted by a re-seed", -1L in removed.captured)
        }

    @Test
    fun `a hand-added word gets an id the asset can never reach`() {
        // The asset numbers from 1 upward, so ids below zero stay clear of it however
        // much the corpus grows.
        assertEquals(-1L, nextUserWordId(lowestExistingId = 1L))
        assertEquals(-1L, nextUserWordId(lowestExistingId = null))
        assertEquals(-3L, nextUserWordId(lowestExistingId = -2L))
    }

    @Test
    fun `preset ids are namespaced away from the shipped ones`() {
        assertEquals("my-kitchen", userPresetId("Kitchen"))
        assertEquals("my-kitchen", userPresetId("  Kitchen  "))
        // Polish diacritics fold, and a repeat gets a suffix rather than colliding.
        assertEquals("my-jedzenie", userPresetId("Jedzenie"))
        assertEquals("my-kitchen-1", userPresetId("Kitchen", suffix = 1))
        assertEquals("my-preset", userPresetId("!!!"))
    }
}
