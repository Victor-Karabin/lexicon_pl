package com.lexicon.data.repository

import com.lexicon.data.remote.image.RemoteImageSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FallbackImageProviderImplTest {
    private val pexels: RemoteImageSource = mockk()
    private val pixabay: RemoteImageSource = mockk()
    private val unsplash: RemoteImageSource = mockk()
    private val openverse: RemoteImageSource = mockk()
    private val provider = FallbackImageProviderImpl(listOf(pexels, pixabay, unsplash, openverse))

    @Before
    fun setUp() {
        coEvery { pexels.searchImageUrl(any()) } returns null
        coEvery { pixabay.searchImageUrl(any()) } returns null
        coEvery { unsplash.searchImageUrl(any()) } returns null
        coEvery { openverse.searchImageUrl(any()) } returns null
    }

    @Test
    fun `returns the first provider's result without trying the rest`() =
        runTest {
            coEvery { pexels.searchImageUrl("kot") } returns "https://pexels/kot.jpg"

            val result = provider.searchImage("kot")

            assertEquals("https://pexels/kot.jpg", result)
            coVerify(exactly = 0) { pixabay.searchImageUrl(any()) }
        }

    @Test
    fun `falls through to the next provider when the previous one has no result`() =
        runTest {
            coEvery { pexels.searchImageUrl("kot") } returns null
            coEvery { pixabay.searchImageUrl("kot") } returns "https://pixabay/kot.jpg"

            val result = provider.searchImage("kot")

            assertEquals("https://pixabay/kot.jpg", result)
        }

    @Test
    fun `falls through to the last source as a last resort`() =
        runTest {
            coEvery { openverse.searchImageUrl("kot") } returns "https://openverse/kot.jpg"

            val result = provider.searchImage("kot")

            assertEquals("https://openverse/kot.jpg", result)
        }

    @Test
    fun `returns null when every provider has no result`() =
        runTest {
            val result = provider.searchImage("kot")

            assertNull(result)
        }

    @Test
    fun `no sources at all is null rather than a crash — a platform may have none wired up`() =
        runTest {
            assertNull(FallbackImageProviderImpl(emptyList()).searchImage("kot"))
        }

    @Test
    fun `candidates are pooled across sources rather than taken from the first that answers`() =
        runTest {
            coEvery { pexels.searchImageUrls("kot", any()) } returns listOf("a", "b")
            coEvery { pixabay.searchImageUrls("kot", any()) } returns listOf("c", "d")

            val result = provider.searchImages("kot", count = 3)

            // One from each in turn, so the first row on screen is never one provider's.
            assertEquals(listOf("a", "c", "b"), result)
        }

    @Test
    fun `asking again offers what has not been shown yet`() =
        runTest {
            coEvery { pexels.searchImageUrls("kot", any()) } returns listOf("a", "b", "c", "d", "e", "f")

            assertEquals(listOf("a", "b", "c"), provider.searchImages("kot", count = 3))
            assertEquals(listOf("d", "e", "f"), provider.searchImages("kot", count = 3, skip = 3))
        }

    @Test
    fun `the same picture from two libraries is only offered once`() =
        runTest {
            coEvery { pexels.searchImageUrls("kot", any()) } returns listOf("a", "b")
            coEvery { pixabay.searchImageUrls("kot", any()) } returns listOf("b", "c")

            assertEquals(listOf("a", "b", "c"), provider.searchImages("kot", count = 3))
        }

    /**
     * Replaces an earlier test that asserted the opposite — that sources past the ones
     * which filled the ask were left alone. Stopping early meant a page of ten was ten
     * variations from whichever provider is listed first, so every source is asked now
     * and the extra calls are the price of a spread.
     */
    @Test
    fun `every source is asked, so a page is a spread rather than one provider's shelf`() =
        runTest {
            coEvery { pexels.searchImageUrls("kot", any()) } returns listOf("p1", "p2", "p3")
            coEvery { pixabay.searchImageUrls("kot", any()) } returns listOf("b1", "b2", "b3")
            coEvery { unsplash.searchImageUrls("kot", any()) } returns listOf("u1", "u2", "u3")
            coEvery { openverse.searchImageUrls("kot", any()) } returns listOf("o1", "o2", "o3")

            assertEquals(listOf("p1", "b1", "u1"), provider.searchImages("kot", count = 3))
            coVerify { pixabay.searchImageUrls("kot", any()) }
        }

    @Test
    fun `a source that throws costs its share of the page, not the page`() =
        runTest {
            coEvery { pexels.searchImageUrls("kot", any()) } throws IllegalStateException("down")
            coEvery { pixabay.searchImageUrls("kot", any()) } returns listOf("b1", "b2")
            coEvery { unsplash.searchImageUrls("kot", any()) } returns emptyList()
            coEvery { openverse.searchImageUrls("kot", any()) } returns emptyList()

            assertEquals(listOf("b1", "b2"), provider.searchImages("kot", count = 3))
        }

    @Test
    fun `running out of pictures is a short list rather than a failure`() =
        runTest {
            coEvery { pexels.searchImageUrls(any(), any()) } returns emptyList()
            coEvery { pixabay.searchImageUrls(any(), any()) } returns listOf("a")
            coEvery { unsplash.searchImageUrls(any(), any()) } returns emptyList()
            coEvery { openverse.searchImageUrls(any(), any()) } returns emptyList()

            assertEquals(listOf("a"), provider.searchImages("kot", count = 3))
            assertEquals(emptyList<String>(), provider.searchImages("kot", count = 3, skip = 3))
        }
}
