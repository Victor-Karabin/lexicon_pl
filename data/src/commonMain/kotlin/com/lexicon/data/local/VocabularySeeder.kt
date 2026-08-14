package com.lexicon.data.local

import com.lexicon.boundary.SyncOutcomeBoundary
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile

class VocabularySeeder(
    private val wordDao: WordDao,
    private val vocabularySeedAssetLoader: VocabularySeedAssetLoader,
    private val vocabularySyncStore: VocabularySyncStore,
) {
    private val mutex = Mutex()

    @Volatile
    private var syncedThisProcess = false

    suspend fun ensureSeeded() {
        if (syncedThisProcess) return
        sync()
    }

    suspend fun sync(): SyncOutcomeBoundary =
        mutex.withLock {
            val fingerprint = vocabularySeedAssetLoader.fingerprint()
            if (fingerprint == vocabularySyncStore.syncedFingerprint() && wordDao.countIncludingDeleted() > 0) {
                syncedThisProcess = true
                return@withLock SyncOutcomeBoundary(total = wordDao.count(), added = 0, updated = 0, removed = 0)
            }

            val outcome = reconcile()
            vocabularySyncStore.setSyncedFingerprint(fingerprint)
            syncedThisProcess = true
            outcome
        }

    private suspend fun reconcile(): SyncOutcomeBoundary {
        val asset = vocabularySeedAssetLoader.load()
        val all = wordDao.getAll()
        // A word the learner wrote, or edited and so took over, is not the asset's to
        // reconcile: the diff below would otherwise read it as one the asset had
        // dropped and delete it, or rewrite the edit back to what ships.
        val existing = all.filterNot { it.isUserCreated }.associateBy { it.id }

        if (all.isEmpty()) {
            wordDao.insertAll(asset)
            return SyncOutcomeBoundary(total = asset.size, added = asset.size, updated = 0, removed = 0)
        }

        val assetIds = asset.mapTo(mutableSetOf()) { it.id }
        // Against every id on file, not just the ones above: an edited shipped word
        // keeps its id and drops out of `existing`, and inserting the asset's copy
        // over it would collide on the primary key.
        val onFile = all.mapTo(mutableSetOf()) { it.id }
        val added = asset.filter { it.id !in onFile }
        val removed = existing.keys - assetIds
        val changed = asset.mapNotNull { incoming ->
            val current = existing[incoming.id] ?: return@mapNotNull null
            incoming
                .copy(isFavourite = current.isFavourite, isDeleted = current.isDeleted)
                .takeIf { it != current }
        }
        wordDao.reconcile(added = added, removedIds = removed.toList(), changed = changed)

        return SyncOutcomeBoundary(
            total = asset.size,
            added = added.size,
            updated = changed.size,
            removed = removed.size,
        )
    }
}
