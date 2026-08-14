package com.lexicon.boundary

interface ImageProvider {
    suspend fun searchImage(query: String): String?

    /**
     * Several candidates to choose between, rather than the single best guess
     * [searchImage] returns. [skip] drops the ones already shown, so asking again
     * offers something new instead of repeating the first batch.
     */
    suspend fun searchImages(
        query: String,
        count: Int,
        skip: Int = 0,
    ): List<String>

    /**
     * Pins [imageUrl] as the image for [query], which is what [searchImage] will
     * return from then on. The trainings look an image up by a word's English
     * translation, so pinning under that key is what makes a chosen image appear
     * in Puzzle, Image Test and Memory Cards.
     */
    suspend fun pinImage(
        query: String,
        imageUrl: String,
    )
}
