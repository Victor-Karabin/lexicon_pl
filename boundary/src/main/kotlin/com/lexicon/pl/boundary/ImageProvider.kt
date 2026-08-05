package com.lexicon.pl.boundary

interface ImageProvider {
    /** Returns a usable image URL for [query], or null if no provider produced one. */
    suspend fun searchImage(query: String): String?
}
