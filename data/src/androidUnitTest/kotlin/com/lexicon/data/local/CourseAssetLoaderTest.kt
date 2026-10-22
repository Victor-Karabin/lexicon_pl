package com.lexicon.data.local

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseAssetLoaderTest {
    @Test
    fun `a build without the course file has an empty course catalogue`() {
        val assets = mockk<AssetReader> { every { exists("course_krok.json") } returns false }
        val loader = CourseAssetLoader(assets)

        assertTrue(loader.load().courses.isEmpty())
        assertEquals(loader.fingerprint(), loader.fingerprint())
        verify(exactly = 0) { assets.readText(any()) }
    }

    @Test
    fun `the course file is read when the build has it`() {
        val assets = mockk<AssetReader> {
            every { exists("course_krok.json") } returns true
            every { readText("course_krok.json") } returns """{"courses": [{"id": "krok-a1", "order": 1, "level": "A1"}]}"""
        }

        assertEquals(listOf("krok-a1"), CourseAssetLoader(assets).load().courses.map { it.id })
    }
}
