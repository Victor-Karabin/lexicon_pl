package com.lexicon.data.repository

import com.lexicon.common.Clock
import com.lexicon.data.local.ImageUrlCacheDao
import com.lexicon.data.local.ImageUrlCacheEntity
import com.lexicon.data.local.NO_IMAGE
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CachingImageProviderImplTest {
    private val now = 1_000_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    private val delegate: FallbackImageProviderImpl = mockk()
    private val imageUrlCacheDao: ImageUrlCacheDao = mockk(relaxed = true)
    private val clock = object : Clock {
        override fun nowEpochMillis(): Long = now

        override fun todayEpochDay(): Long = now / day
    }
    private val provider = CachingImageProviderImpl(delegate, imageUrlCacheDao, clock)

    @Test
    fun `a cached picture is returned without asking the web`() =
        runTest {
            coEvery { imageUrlCacheDao.find("kot") } returns ImageUrlCacheEntity("kot", "https://cache/kot.jpg")

            assertEquals("https://cache/kot.jpg", provider.searchImage("kot"))
            coVerify(exactly = 0) { delegate.lookUp(any()) }
        }

    @Test
    fun `a picture found on the web is cached`() =
        runTest {
            coEvery { imageUrlCacheDao.find("kot") } returns null
            coEvery { delegate.lookUp("kot") } returns ImageLookup.Found("https://provider/kot.jpg")

            assertEquals("https://provider/kot.jpg", provider.searchImage("kot"))
            coVerify { imageUrlCacheDao.insert(ImageUrlCacheEntity("kot", "https://provider/kot.jpg", now)) }
        }

    @Test
    fun `a word every source answered for with nothing is not searched again for a week`() =
        runTest {
            coEvery { imageUrlCacheDao.find("że") } returns null
            coEvery { delegate.lookUp("że") } returns ImageLookup.NoneFound

            assertNull(provider.searchImage("że"))
            coVerify { imageUrlCacheDao.insert(ImageUrlCacheEntity("że", NO_IMAGE, now)) }

            coEvery { imageUrlCacheDao.find("że") } returns ImageUrlCacheEntity("że", NO_IMAGE, now - 6 * day)
            assertNull(provider.searchImage("że"))
            coVerify(exactly = 1) { delegate.lookUp("że") }

            coEvery { imageUrlCacheDao.find("że") } returns ImageUrlCacheEntity("że", NO_IMAGE, now - 8 * day)
            provider.searchImage("że")
            coVerify(exactly = 2) { delegate.lookUp("że") }
        }

    @Test
    fun `a search that failed, offline say, is not remembered as having no picture`() =
        runTest {
            coEvery { imageUrlCacheDao.find("kot") } returns null
            coEvery { delegate.lookUp("kot") } returns ImageLookup.Unavailable

            assertNull(provider.searchImage("kot"))
            coVerify(exactly = 0) { imageUrlCacheDao.insert(any()) }
        }
}
