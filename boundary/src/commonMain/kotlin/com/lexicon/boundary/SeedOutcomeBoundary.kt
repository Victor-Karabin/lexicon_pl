package com.lexicon.boundary

data class SeedOutcomeBoundary(
    val total: Int,
    val added: Int,
    val updated: Int,
    val removed: Int,
)

val SeedOutcomeBoundary.wasAlreadyCurrent: Boolean get() = added == 0 && updated == 0 && removed == 0
