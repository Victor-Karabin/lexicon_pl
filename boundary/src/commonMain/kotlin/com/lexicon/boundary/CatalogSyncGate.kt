package com.lexicon.boundary

interface CatalogSyncGate {
    suspend fun isCurrent(): Boolean

    suspend fun markCurrent()
}
