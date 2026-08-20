package com.lexicon.boundary

interface ImageProvider {
    suspend fun searchImage(query: String): String?

    suspend fun searchImages(
        query: String,
        count: Int,
        skip: Int = 0,
    ): List<String>

    suspend fun pinImage(
        query: String,
        imageUrl: String,
    )
}
