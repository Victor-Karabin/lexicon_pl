package com.lexicon.data.repository

import com.lexicon.boundary.AppVersionProvider
import com.lexicon.boundary.CatalogSyncGate
import com.lexicon.data.local.VocabularySyncStore

class CatalogSyncGateImpl(
    private val store: VocabularySyncStore,
    private val appVersion: AppVersionProvider,
) : CatalogSyncGate {
    override suspend fun isCurrent(): Boolean = store.syncedAppVersion() == appVersion.versionCode()

    override suspend fun markCurrent() = store.setSyncedAppVersion(appVersion.versionCode())
}
