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
}
