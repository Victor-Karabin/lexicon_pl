package com.lexicon.boundary

interface CatalogSeedGate {
    suspend fun isCurrent(): Boolean

    suspend fun markCurrent()
}
