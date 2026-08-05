package com.lexicon.pl.boundary

interface TrainingHistoryRepository {
    suspend fun recordResult(result: TrainingResultBoundary)
}
