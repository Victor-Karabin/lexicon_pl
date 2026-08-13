package com.lexicon.boundary

data class SyncOutcomeBoundary(
    val total: Int,
    val added: Int,
    val updated: Int,
    val removed: Int,
)

val SyncOutcomeBoundary.wasAlreadyCurrent: Boolean get() = added == 0 && updated == 0 && removed == 0
