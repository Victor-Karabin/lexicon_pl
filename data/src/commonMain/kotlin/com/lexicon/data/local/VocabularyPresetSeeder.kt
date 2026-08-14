package com.lexicon.data.local

import com.lexicon.boundary.SyncOutcomeBoundary
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile

class VocabularyPresetSeeder(
    private val presetDao: PresetDao,
    private val loader: VocabularyPresetAssetLoader,
    private val syncStore: VocabularySyncStore,
) {
    private val mutex = Mutex()

    @Volatile
    private var syncedThisProcess = false

    suspend fun ensureSeeded() {
        if (syncedThisProcess) return
        sync()
    }

    suspend fun restore(presetId: String) {
        presetDao.undeletePreset(presetId)
        syncStore.setSyncedPresetFingerprint("")
        syncedThisProcess = false
        ensureSeeded()
    }

    suspend fun delete(presetId: String) {
        presetDao.insertDeletedPreset(DeletedPresetEntity(presetId))
        presetDao.deleteMemberships(presetId)
        presetDao.deletePreset(presetId)
    }

    suspend fun sync(): SyncOutcomeBoundary =
        mutex.withLock {
            val fingerprint = loader.fingerprint()
            val stored = presetDao.countPresets()
            if (fingerprint == syncStore.syncedPresetFingerprint() && stored > 0) {
                syncedThisProcess = true
                return@withLock SyncOutcomeBoundary(total = stored, added = 0, updated = 0, removed = 0)
            }

            val catalog = loader.load()
            val deleted = presetDao.getDeletedPresetIds().toSet()
            val presets = catalog.presets.filterNot { it.id in deleted }
            presetDao.replaceCatalog(
                categories = catalog.categories.map { it.toEntity() },
                presets = presets.map { it.toEntity() },
                memberships = presets.flatMap { it.toMemberships() },
            )
            syncStore.setSyncedPresetFingerprint(fingerprint)
            syncedThisProcess = true

            SyncOutcomeBoundary(
                total = presets.size,
                added = presets.size - stored.coerceAtMost(presets.size),
                updated = stored.coerceAtMost(presets.size),
                removed = (stored - presets.size).coerceAtLeast(0),
            )
        }
}
