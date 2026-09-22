package com.lexicon.application.training

import com.lexicon.boundary.ImageProvider
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageLookupsTest {
    private val asked = mutableListOf<String>()

    private val slowProvider = object : ImageProvider {
        override suspend fun searchImage(query: String): String? {
            asked += query
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
            val pictures = slowProvider.picturesFor(listOf(word("cat"), word("dog"), word("bird")))

            assertEquals(listOf("https://img/cat.jpg", "https://img/dog.jpg", "https://img/bird.jpg"), pictures)
            assertEquals(LOOKUP_MS, currentTime)
        }

    @Test
    fun `a lookup that fails leaves that word without a picture, not the others`() =
        runTest {
            val pictures = slowProvider.picturesFor(listOf(word("cat"), word("broken"), word("nothing")))

            assertEquals(listOf("https://img/cat.jpg", null, null), pictures)
        }

    @Test
    fun `a word with a picture phrase is searched by the phrase, not its gloss`() =
        runTest {
            val lilac = word("lilac", picture = "lilac flowers in bloom")

            assertEquals("https://img/lilac flowers in bloom.jpg", slowProvider.pictureOf(lilac))
            assertEquals(listOf("lilac flowers in bloom"), asked)
        }

    @Test
    fun `a word no photo can show gets no picture and no search`() =
        runTest {
            val without = word("without", picture = "")

            assertEquals(listOf(null), slowProvider.picturesFor(listOf(without)))
            assertEquals(emptyList<String>(), asked)
        }

    private fun word(
        translation: String,
        picture: String? = null,
    ) = Word(VocabularyId(translation.hashCode().toLong()), "słowo", translation, "", picture = picture)

    private companion object {
        const val LOOKUP_MS = 1_000L
    }
}
