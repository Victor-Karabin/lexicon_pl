package com.lexicon.data.repository

import com.lexicon.data.local.ImageUrlCacheDao
import com.lexicon.data.local.ImageUrlCacheEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CachingImageProviderImplTest {
    private val delegate: FallbackImageProviderImpl = mockk()
    private val imageUrlCacheDao: ImageUrlCacheDao = mockk()
    private val provider = CachingImageProviderImpl(delegate, imageUrlCacheDao)

    @Test
    fun `returns the cached URL without calling the delegate`() =
        runTest {
            coEvery { imageUrlCacheDao.get("kot") } returns "https://cache/kot.jpg"

            val result = provider.searchImage("kot")

            assertEquals("https://cache/kot.jpg", result)
            coVerify(exactly = 0) { delegate.searchImage(any()) }
        }

    @Test
    fun `on a cache miss, fetches from the delegate and stores the result`() =
        runTest {
            coEvery { imageUrlCacheDao.get("kot") } returns null
            coEvery { delegate.searchImage("kot") } returns "https://provider/kot.jpg"
            coEvery { imageUrlCacheDao.insert(any()) } returns Unit

            val result = provider.searchImage("kot")

            assertEquals("https://provider/kot.jpg", result)
            coVerify { imageUrlCacheDao.insert(ImageUrlCacheEntity(query = "kot", imageUrl = "https://provider/kot.jpg")) }
        }

    @Test
    fun `on a cache miss with no delegate result, returns null and stores nothing`() =
        runTest {
            coEvery { imageUrlCacheDao.get("kot") } returns null
            coEvery { delegate.searchImage("kot") } returns null

            val result = provider.searchImage("kot")

            assertNull(result)
            coVerify(exactly = 0) { imageUrlCacheDao.insert(any()) }
        }
}
