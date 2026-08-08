package com.lexicon.boundary

/**
 * What a sync did, in the terms the splash screen reports it: how many rows the store now holds
 * and how it got there. Counts rather than a bare success flag, so "up to date" and "just
 * imported two thousand words" are distinguishable — they take very different amounts of time,
 * and a user watching a progress screen deserves to know which one they are waiting for.
 */
data class SyncOutcomeBoundary(
    val total: Int,
    val added: Int,
    val updated: Int,
    val removed: Int,
) {
    val wasAlreadyCurrent: Boolean get() = added == 0 && updated == 0 && removed == 0
}
