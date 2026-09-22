package com.lexicon.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CropWindowTest {
    @Test
    fun `a portrait photo is cut to the card's shape and moves up and down`() {
        val window = CropWindow(imageWidth = 1200, imageHeight = 1600)

        assertTrue(window.needsPositioning)
        assertFalse(window.movesHorizontally)
        assertEquals(CropRect(left = 0, top = 0, width = 1200, height = 750), window.rectAt(0f))
        assertEquals(CropRect(left = 0, top = 850, width = 1200, height = 750), window.rectAt(1f))
        assertEquals(CropRect(left = 0, top = 425, width = 1200, height = 750), window.rectAt(0.5f))
    }

    @Test
    fun `a panorama keeps its full height and moves side to side`() {
        val window = CropWindow(imageWidth = 3000, imageHeight = 1000)

        assertTrue(window.movesHorizontally)
        assertEquals(CropRect(left = 1400, top = 0, width = 1600, height = 1000), window.rectAt(1f))
    }

    @Test
    fun `a photo already the card's shape is taken as it is`() {
        assertFalse(CropWindow(imageWidth = 1600, imageHeight = 1000).needsPositioning)
        assertFalse(CropWindow(imageWidth = 1620, imageHeight = 1000).needsPositioning)
        assertTrue(CropWindow(imageWidth = 1000, imageHeight = 1000).needsPositioning)
    }

    @Test
    fun `dragging the frame down moves it down the photo, and stops at the edge`() {
        val window = CropWindow(imageWidth = 1200, imageHeight = 1600)

        val shownHeight = 400f
        val moved = window.focusAfterFrameDrag(focus = 0.5f, dragPixels = 100f, shownImagePixels = shownHeight)

        assertEquals(0.5f + (100f * 1600 / shownHeight) / 850, moved, 0.0001f)
        assertEquals(1f, window.focusAfterFrameDrag(focus = 0.9f, dragPixels = 1000f, shownImagePixels = shownHeight), 0f)
        assertEquals(0f, window.focusAfterFrameDrag(focus = 0.1f, dragPixels = -1000f, shownImagePixels = shownHeight), 0f)
    }
}
