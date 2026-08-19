package com.lexicon.data.repository

import com.lexicon.boundary.AppVersionProvider
import com.lexicon.boundary.CatalogSeedGate
import com.lexicon.data.local.CatalogSeedStore

class CatalogSeedGateImpl(
    private val store: CatalogSeedStore,
    private val appVersion: AppVersionProvider,
) : CatalogSeedGate {
    override suspend fun isCurrent(): Boolean = store.syncedAppVersion() == appVersion.versionCode()

    override suspend fun markCurrent() = store.setSyncedAppVersion(appVersion.versionCode())
}
