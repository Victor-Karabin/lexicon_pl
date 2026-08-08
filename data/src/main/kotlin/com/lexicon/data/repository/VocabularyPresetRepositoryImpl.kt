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
