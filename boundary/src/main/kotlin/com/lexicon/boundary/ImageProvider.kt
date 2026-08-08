package com.lexicon.boundary

interface ImageProvider {
    suspend fun searchImage(query: String): String?
}
