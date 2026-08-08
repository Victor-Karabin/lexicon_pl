package com.lexicon.data.repository

import com.lexicon.boundary.PresetCategoryBoundary
import com.lexicon.boundary.SyncOutcomeBoundary
import com.lexicon.boundary.VocabularyPresetBoundary
import com.lexicon.boundary.VocabularyPresetRepository
import com.lexicon.data.local.PresetDao
import com.lexicon.data.local.VocabularyPresetSeeder
import com.lexicon.data.local.toBoundary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Serves the preset catalogue from the database.
 *
 * Reading rows rather than re-parsing the asset means listing presets costs a query instead of
 * decoding a megabyte of JSON, and the same store can later hold presets that never came from
 * an asset at all — downloaded, imported or user-made.
 */
@Singleton
class VocabularyPresetRepositoryImpl
    @Inject
    constructor(
        private val presetDao: PresetDao,
        private val seeder: VocabularyPresetSeeder,
    ) : VocabularyPresetRepository {
        override suspend fun syncFromSource(): SyncOutcomeBoundary = seeder.sync()

        override suspend fun getPresets(): List<VocabularyPresetBoundary> {
            seeder.ensureSeeded()
            // Memberships are fetched in one query and grouped here: a per-preset query would be
            // 72 round trips to draw one screen.
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

        override suspend fun deletePreset(id: String) {
            seeder.ensureSeeded()
            seeder.delete(id)
        }

        override suspend fun restorePreset(id: String) = seeder.restore(id)

        override suspend fun getCategories(): List<PresetCategoryBoundary> {
            seeder.ensureSeeded()
            return presetDao.getCategories().map { it.toBoundary() }
        }
    }
