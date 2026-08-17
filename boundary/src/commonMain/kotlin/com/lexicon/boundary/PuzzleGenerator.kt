package com.lexicon.boundary

data class FillwordRequestBoundary(
    val words: List<String>,
    val gridSize: Int,
    val difficulty: String,
)

data class FillwordPlacementBoundary(
    val word: String,
    val startRow: Int,
    val startColumn: Int,
    val direction: String,
)

sealed interface FillwordResultBoundary {
    data class Generated(
        val grid: List<List<String>>,
        val placements: List<FillwordPlacementBoundary>,
    ) : FillwordResultBoundary

    data object Offline : FillwordResultBoundary

    data class Refused(val reason: String) : FillwordResultBoundary
}

interface FillwordGenerator {
    suspend fun generate(request: FillwordRequestBoundary): FillwordResultBoundary
}
