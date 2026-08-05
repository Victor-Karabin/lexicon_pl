package com.lexicon.boundary

interface TrainingHistoryRepository {
    suspend fun recordResult(result: TrainingResultBoundary)
}
