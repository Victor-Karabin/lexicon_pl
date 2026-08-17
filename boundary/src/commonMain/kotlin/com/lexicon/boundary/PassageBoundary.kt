package com.lexicon.boundary

data class PassageBoundary(
    val id: String,
    val title: String,
    val cefr: String,
    val text: String,
    val keyWords: List<String>,
)

interface PassageRepository {
    suspend fun passages(): List<PassageBoundary>
}
