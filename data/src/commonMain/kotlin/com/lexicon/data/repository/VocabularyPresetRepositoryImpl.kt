package com.lexicon.data.repository

import com.lexicon.boundary.PresetCategoryBoundary
import com.lexicon.boundary.SeedOutcomeBoundary
import com.lexicon.boundary.VocabularyPresetBoundary
import com.lexicon.boundary.VocabularyPresetRepository
import com.lexicon.data.local.PresetDao
import com.lexicon.data.local.PresetEntity
import com.lexicon.data.local.USER_PRESET_CATEGORY_ID
import com.lexicon.data.local.VocabularyPresetSeeder
import com.lexicon.data.local.encodeLocalized
import com.lexicon.data.local.toBoundary
import com.lexicon.data.local.userPresetId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

private const val SLUG_LANGUAGE = "en"

class VocabularyPresetRepositoryImpl(
    private val presetDao: PresetDao,
    private val seeder: VocabularyPresetSeeder,
) : VocabularyPresetRepository {
    override suspend fun seedFromAsset(): SeedOutcomeBoundary = seeder.sync()

    override suspend fun getPresets(): List<VocabularyPresetBoundary> {
        seeder.ensureSeeded()
        val membership = presetDao.getAllMemberships().groupBy { it.presetId }
        return presetDao.getPresets().map { preset ->
            preset.toBoundary(membership[preset.id].orEmpty().map { it.wordId })
        }
    }

    override fun observePresets(): Flow<List<VocabularyPresetBoundary>> =
        presetDao
            .observeMemberships()
            .onStart { seeder.ensureSeeded() }
            .map { memberships ->
                val byPreset = memberships.groupBy { it.presetId }
                presetDao.getPresets().map { preset ->
                    preset.toBoundary(byPreset[preset.id].orEmpty().map { it.wordId })
                }
            }

    override suspend fun getPreset(id: String): VocabularyPresetBoundary? {
        seeder.ensureSeeded()
        val preset = presetDao.getPreset(id) ?: return null
        return preset.toBoundary(presetDao.getWordIds(id))
    }

    override suspend fun createPreset(
        title: Map<String, String>,
        description: Map<String, String>,
        icon: String?,
        color: String?,
        wordIds: List<Long>,
    ): VocabularyPresetBoundary {
        seeder.ensureSeeded()
        val preset = PresetEntity(
            id = freeUserPresetId(title[SLUG_LANGUAGE] ?: title.values.firstOrNull().orEmpty()),
            categoryId = USER_PRESET_CATEGORY_ID,
            titleJson = title.encodeLocalized(),
            descriptionJson = description.encodeLocalized(),
            icon = icon,
            color = color,
            popularity = presetDao.countUserPresets(),
            estimatedSeconds = 0,
            isUserCreated = true,
        )
        presetDao.createUserPreset(preset, wordIds.distinct())
        return preset.toBoundary(wordIds.distinct())
    }

    private suspend fun freeUserPresetId(title: String): String {
        var suffix = 0
        while (true) {
            val candidate = userPresetId(title, suffix)
            if (presetDao.countPresetsWithId(candidate) == 0) return candidate
            suffix++
        }
    }

    override suspend fun deletePreset(id: String) {
        seeder.ensureSeeded()
        seeder.delete(id)
    }

    override suspend fun restorePreset(id: String) = seeder.restore(id)

    override suspend fun getPresetIdsForWord(wordId: Long): List<String> {
        seeder.ensureSeeded()
        return presetDao.getPresetIdsForWord(wordId)
    }

    override suspend fun setWordInPreset(
        presetId: String,
        wordId: Long,
        isMember: Boolean,
    ) {
        seeder.ensureSeeded()
        presetDao.setWordInPreset(presetId, wordId, isMember)
    }

    override suspend fun getCategories(): List<PresetCategoryBoundary> {
        seeder.ensureSeeded()
        return presetDao.getCategories().map { it.toBoundary() }
    }
}
