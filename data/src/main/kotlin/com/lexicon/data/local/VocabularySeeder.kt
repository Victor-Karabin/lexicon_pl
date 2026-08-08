package com.lexicon.data.local

import com.lexicon.boundary.SyncOutcomeBoundary
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the words table in line with the bundled asset.
 *
 * Seeding an empty table is only the first-run case. The corpus also *changes* between
 * releases — words added, corrected, or dropped — and an install that merely checked "is the
 * table empty?" would sit on whatever it was first seeded with for ever. That is exactly what
 * happened when the vocabulary grew from 1,767 to 2,219 words: existing installs saw none of
 * the new ones, and the levels those words filled in stayed empty.
 *
 * Reconciling rather than reseeding matters because the rows carry the user's favourites,
 * which are theirs and not the asset's to overwrite.
 */
@Singleton
class VocabularySeeder
    @Inject
    constructor(
        private val wordDao: WordDao,
        private val vocabularySeedAssetLoader: VocabularySeedAssetLoader,
        private val vocabularySyncStore: VocabularySyncStore,
    ) {
        private val mutex = Mutex()

        @Volatile
        private var syncedThisProcess = false

        /** Called by anything that reads words, and cheap once the process has synced. */
        suspend fun ensureSeeded() {
            if (syncedThisProcess) return
            sync()
        }

        /** Reports what it did, for the startup screen; [ensureSeeded] is the silent form. */
        suspend fun sync(): SyncOutcomeBoundary =
            mutex.withLock {
                val fingerprint = vocabularySeedAssetLoader.fingerprint()
                // The common launch: the asset has not moved since the last sync, so nothing is
                // parsed and nothing is read. The row count is still checked, because a schema
                // change can empty the table without the asset changing at all.
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
            val existing = wordDao.getAll().associateBy { it.id }

            if (existing.isEmpty()) {
                wordDao.insertAll(asset)
                return SyncOutcomeBoundary(total = asset.size, added = asset.size, updated = 0, removed = 0)
            }

            val assetIds = asset.mapTo(mutableSetOf()) { it.id }
            val added = asset.filter { it.id !in existing }
            wordDao.insertAll(added)

            // A word the asset no longer carries goes, and takes its heart with it — there is
            // no longer anything to study.
            val removed = existing.keys - assetIds
            if (removed.isNotEmpty()) wordDao.deleteByIds(removed.toList())

            // Everything else is refreshed from the asset except what the user decided: their
            // favourites, and their deletions. Both are theirs and not the asset's to state, and
            // a deletion in particular would otherwise undo itself on the very next launch. Only
            // rows that actually differ are written, so a corrected translation costs one update
            // rather than two thousand.
            val changed = asset.mapNotNull { incoming ->
                val current = existing[incoming.id] ?: return@mapNotNull null
                incoming
                    .copy(isFavourite = current.isFavourite, isDeleted = current.isDeleted)
                    .takeIf { it != current }
            }
            if (changed.isNotEmpty()) wordDao.updateAll(changed)

            return SyncOutcomeBoundary(
                total = asset.size,
                added = added.size,
                updated = changed.size,
                removed = removed.size,
            )
        }
    }
