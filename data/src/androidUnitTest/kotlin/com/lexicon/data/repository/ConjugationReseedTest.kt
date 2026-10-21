package com.lexicon.data.repository

import com.lexicon.data.local.AssetReader
import com.lexicon.data.local.CatalogSeedStore
import com.lexicon.data.local.ConjugationAssetLoader
import com.lexicon.data.local.ConjugationDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ConjugationReseedTest {
    private fun asset(first: String) = """[{"bezokolicznik": "mieć", "ja": "$first"}, {"bezokolicznik": "być", "ja": "jestem"}]"""

    private fun loader(text: String) = ConjugationAssetLoader(mockk<AssetReader> { every { readText("conjugations.json") } returns text })

    private val dao: ConjugationDao = mockk(relaxed = true) { coEvery { countVerbs() } returns 2 }
    private val syncStore: CatalogSeedStore = mockk(relaxed = true)

    private fun repository(text: String) = ConjugationRepositoryImpl(loader(text), dao, syncStore)

    @Test
    fun `an installed app that already holds the asset does not seed again`() =
        runTest {
            coEvery { syncStore.syncedVerbFingerprint() } returns loader(asset("mam")).fingerprint()

            repository(asset("mam")).seedFromAsset()

            coVerify(exactly = 0) { dao.saveVerbs(any()) }
        }

    @Test
    fun `a changed asset of the same length reaches an installed app`() =
        runTest {
            coEvery { syncStore.syncedVerbFingerprint() } returns loader(asset("mam")).fingerprint()

            repository(asset("maM")).seedFromAsset()

            coVerify(exactly = 1) { dao.saveVerbs(any()) }
        }
}
