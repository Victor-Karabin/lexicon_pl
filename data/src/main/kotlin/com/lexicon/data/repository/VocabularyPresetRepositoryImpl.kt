package com.lexicon.data.repository

import com.lexicon.boundary.PresetCategoryBoundary
import com.lexicon.boundary.VocabularyPresetBoundary
import com.lexicon.boundary.VocabularyPresetRepository
import com.lexicon.data.local.VocabularyPresetAssetLoader
import com.lexicon.data.local.toBoundary
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Serves the bundled catalogue, parsed once.
 *
 * The asset holds every preset's full id list — a megabyte-scale parse for a thousand-word
 * preset — so it is read on first use and kept, behind a mutex so a burst of concurrent
 * calls at startup cannot each trigger their own parse.
 */
@Singleton
class VocabularyPresetRepositoryImpl
    @Inject
    constructor(
        private val loader: VocabularyPresetAssetLoader,
    ) : VocabularyPresetRepository {
        private val mutex = Mutex()

        @Volatile
        private var cached: com.lexicon.boundary.VocabularyPresetCatalogBoundary? = null

        private suspend fun catalog() =
            cached ?: mutex.withLock {
                cached ?: loader.load().toBoundary().also { cached = it }
            }

        override suspend fun getPresets(): List<VocabularyPresetBoundary> = catalog().presets

        override suspend fun getPreset(id: String): VocabularyPresetBoundary? = catalog().presets.firstOrNull { it.id == id }

        override suspend fun getCategories(): List<PresetCategoryBoundary> = catalog().categories
    }
