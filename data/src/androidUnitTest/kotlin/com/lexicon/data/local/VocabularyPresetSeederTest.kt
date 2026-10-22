package com.lexicon.data.local

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class VocabularyPresetSeederTest {
    private val catalog = """
        {
          "categories": [{"id": "tricky", "order": 2, "title": {"en": "Tricky words"}}],
          "presets": [{"id": "false-friends", "category": "tricky", "vocabularyIds": [2563, 2564]}]
        }
    """.trimIndent()

    private val assets = mockk<AssetReader> { every { readText(any()) } returns catalog }
    private val presetDao = mockk<PresetDao>(relaxed = true) {
        coEvery { countPresets() } returns 0
        coEvery { getDeletedPresetIds() } returns emptyList()
    }
    private val syncStore = mockk<CatalogSeedStore>(relaxed = true) {
        coEvery { syncedPresetFingerprint() } returns null
    }
    private val vocabularySeeder = mockk<VocabularySeeder>(relaxed = true)

    private val seeder = VocabularyPresetSeeder(presetDao, VocabularyPresetAssetLoader(assets), syncStore, vocabularySeeder)

    @Test
    fun `the words a preset links to are brought up to date before the preset is`() =
        runTest {
            seeder.sync()

            coVerifyOrder {
                vocabularySeeder.ensureSeeded()
                presetDao.replaceCatalog(any(), any(), any())
            }
        }

    @Test
    fun `a preset catalogue already in sync still makes sure its words are`() =
        runTest {
            coEvery { presetDao.countPresets() } returns 1
            coEvery { syncStore.syncedPresetFingerprint() } returns VocabularyPresetAssetLoader(assets).fingerprint()

            seeder.sync()

            coVerify { vocabularySeeder.ensureSeeded() }
            coVerify(exactly = 0) { presetDao.replaceCatalog(any(), any(), any()) }
        }

    @Test
    fun `undoing the delete of the learner's own preset brings it back with its words in order`() =
        runTest {
            val mine = PresetEntity(
                id = "my-kitchen",
                categoryId = USER_PRESET_CATEGORY_ID,
                titleJson = "{}",
                descriptionJson = "{}",
                icon = null,
                color = null,
                popularity = 0,
                estimatedSeconds = 0,
                isUserCreated = true,
            )
            coEvery { presetDao.getPreset("my-kitchen") } returns mine
            coEvery { presetDao.getMembershipsOf("my-kitchen") } returns
                listOf(PresetWordEntity("my-kitchen", 7L, position = 0), PresetWordEntity("my-kitchen", 3L, position = 1))

            seeder.delete("my-kitchen")
            seeder.restore("my-kitchen")

            coVerify { presetDao.createUserPreset(mine, listOf(7L, 3L)) }
        }

    @Test
    fun `undoing the delete of a shipped preset re-seeds it from the catalogue`() =
        runTest {
            coEvery { presetDao.getPreset("false-friends") } returns null

            seeder.delete("false-friends")
            seeder.restore("false-friends")

            coVerify { presetDao.undeletePreset("false-friends") }
            coVerify { presetDao.replaceCatalog(any(), any(), any()) }
            coVerify(exactly = 0) { presetDao.createUserPreset(any(), any()) }
        }
}
