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
}
