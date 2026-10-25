package com.lexicon.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val FOOD = "food"

@RunWith(AndroidJUnit4::class)
class PresetDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var presets: PresetDao

    @Before
    fun open() {
        database = Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .build()
        presets = database.presetDao()
    }

    @After
    fun close() = database.close()

    private val category = PresetCategoryEntity(id = "topics", sortOrder = 0, titleJson = "{}")

    private val food = PresetEntity(
        id = FOOD,
        categoryId = "topics",
        titleJson = "{}",
        descriptionJson = "{}",
        icon = null,
        color = null,
        popularity = 0,
        estimatedSeconds = 0,
    )

    private val catalogueWords = listOf(1L, 2L, 3L)

    private suspend fun seedCatalogue() =
        presets.replaceCatalog(
            categories = listOf(category),
            presets = listOf(food),
            memberships = catalogueWords.mapIndexed { position, wordId -> PresetWordEntity(FOOD, wordId, position) },
        )

    private suspend fun order(): List<Long> = presets.getMembershipsOf(FOOD).map { it.wordId }

    @Test
    fun aCatalogueWordAddedBackReturnsToItsCataloguePlaceAndAddedWordsKeepTheirOrder() =
        runTest {
            seedCatalogue()
            presets.setWordInPreset(FOOD, 2L, isMember = false)
            presets.setWordInPreset(FOOD, 2L, isMember = true)
            presets.setWordInPreset(FOOD, 9L, isMember = true)
            presets.setWordInPreset(FOOD, 8L, isMember = true)

            seedCatalogue()
            assertEquals(listOf(1L, 2L, 3L, 9L, 8L), order())

            seedCatalogue()
            assertEquals(listOf(1L, 2L, 3L, 9L, 8L), order())
        }

    @Test
    fun wordsAddedToTheLearnersOwnPresetKeepTheirOrderAcrossCatalogueUpdates() =
        runTest {
            seedCatalogue()
            val mine = food.copy(id = "my-kitchen", categoryId = USER_PRESET_CATEGORY_ID, isUserCreated = true)
            presets.createUserPreset(mine, listOf(5L, 6L))
            presets.setWordInPreset(mine.id, 7L, isMember = true)
            presets.setWordInPreset(mine.id, 5L, isMember = false)
            presets.setWordInPreset(mine.id, 5L, isMember = true)

            seedCatalogue()
            seedCatalogue()

            assertEquals(listOf(6L, 7L, 5L), presets.getMembershipsOf(mine.id).map { it.wordId })
        }
}
