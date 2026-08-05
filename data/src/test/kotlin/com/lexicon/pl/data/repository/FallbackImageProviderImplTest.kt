package com.lexicon.pl.data.repository

import com.lexicon.pl.data.remote.image.OpenverseImageSource
import com.lexicon.pl.data.remote.image.PexelsImageSource
import com.lexicon.pl.data.remote.image.PixabayImageSource
import com.lexicon.pl.data.remote.image.UnsplashImageSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FallbackImageProviderImplTest {
    private val pexels: PexelsImageSource = mockk()
    private val pixabay: PixabayImageSource = mockk()
    private val unsplash: UnsplashImageSource = mockk()
    private val openverse: OpenverseImageSource = mockk()
    private val provider = FallbackImageProviderImpl(pexels, pixabay, unsplash, openverse)

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
    fun `falls through to Openverse as the last resort`() =
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
}
