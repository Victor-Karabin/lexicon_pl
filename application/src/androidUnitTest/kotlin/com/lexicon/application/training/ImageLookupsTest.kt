package com.lexicon.application.training

import com.lexicon.boundary.ImageProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageLookupsTest {
    private val slowProvider = object : ImageProvider {
        override suspend fun searchImage(query: String): String? {
            delay(LOOKUP_MS)
            if (query == "broken") error("the source fell over")
            return if (query == "nothing") null else "https://img/$query.jpg"
        }

        override suspend fun searchImages(
            query: String,
            count: Int,
            skip: Int,
        ): List<String> = emptyList()

        override suspend fun pinImage(
            query: String,
            imageUrl: String,
        ) = Unit
    }

    @Test
    fun `pictures for several words are looked up at the same time`() =
        runTest {
            val pictures = slowProvider.picturesFor(listOf("cat", "dog", "bird"))

            assertEquals(listOf("https://img/cat.jpg", "https://img/dog.jpg", "https://img/bird.jpg"), pictures)
            assertEquals(LOOKUP_MS, currentTime)
        }

    @Test
    fun `a lookup that fails leaves that word without a picture, not the others`() =
        runTest {
            val pictures = slowProvider.picturesFor(listOf("cat", "broken", "nothing"))

            assertEquals(listOf("https://img/cat.jpg", null, null), pictures)
        }

    private companion object {
        const val LOOKUP_MS = 1_000L
    }
}
