package com.lexicon.data.repository

import com.lexicon.data.local.PresetDao
import com.lexicon.data.local.PresetEntity
import com.lexicon.data.local.UNSORTED_PRESET_ID
import com.lexicon.data.local.VocabularyPresetSeeder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UnsortedPresetTest {
    private val presetDao: PresetDao = mockk(relaxed = true)
    private val seeder: VocabularyPresetSeeder = mockk(relaxed = true)
    private val repository = VocabularyPresetRepositoryImpl(presetDao, seeder)

    private val food = PresetEntity("food", "everyday-life", "{}", "{}", null, null, 1, 0)

    @Test
    fun `unsorted comes first and lists the words that are in no preset`() =
        runTest {
            coEvery { presetDao.getPresets() } returns listOf(food)
            coEvery { presetDao.getUnsortedWordIds() } returns listOf(5L, 9L)

            val presets = repository.getPresets()

            assertEquals(listOf(UNSORTED_PRESET_ID, "food"), presets.map { it.id })
            assertEquals(listOf(5L, 9L), presets.first().vocabularyIds)
            assertEquals(listOf(5L, 9L), repository.getPreset(UNSORTED_PRESET_ID)?.vocabularyIds)
            assertEquals(UNSORTED_PRESET_ID, repository.getCategories().first().id)
        }

    @Test
    fun `unsorted can be neither deleted nor given words by hand`() =
        runTest {
            repository.deletePreset(UNSORTED_PRESET_ID)
            repository.setWordInPreset(UNSORTED_PRESET_ID, wordId = 5L, isMember = true)

            coVerify(exactly = 0) { seeder.delete(any()) }
            coVerify(exactly = 0) { presetDao.setWordInPreset(any(), any(), any()) }
        }
}
