package com.lexicon.presentation.presets

import kotlin.math.abs
import kotlin.math.roundToInt

const val CARD_IMAGE_ASPECT = 1.6f

private const val ASPECT_TOLERANCE = 0.03f

data class CropRect(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)

data class CropWindow(
    val imageWidth: Int,
    val imageHeight: Int,
    val aspect: Float = CARD_IMAGE_ASPECT,
) {
    private val isWider: Boolean get() = imageWidth.toFloat() / imageHeight > aspect

    val width: Int get() = if (isWider) (imageHeight * aspect).roundToInt().coerceAtMost(imageWidth) else imageWidth

    val height: Int get() = if (isWider) imageHeight else (imageWidth / aspect).roundToInt().coerceAtMost(imageHeight)

    val slack: Int get() = if (isWider) imageWidth - width else imageHeight - height

    val movesHorizontally: Boolean get() = isWider

    val needsPositioning: Boolean
        get() = imageWidth > 0 && imageHeight > 0 && abs(imageWidth.toFloat() / imageHeight - aspect) / aspect > ASPECT_TOLERANCE

    fun rectAt(focus: Float): CropRect {
        val offset = (slack * focus.coerceIn(0f, 1f)).roundToInt()
        return if (isWider) {
            CropRect(left = offset, top = 0, width = width, height = height)
        } else {
            CropRect(left = 0, top = offset, width = width, height = height)
        }
    }

    fun focusAfterDrag(
        focus: Float,
        dragPixels: Float,
        framePixels: Float,
    ): Float {
        if (slack == 0 || framePixels <= 0f) return focus
        val windowPixels = if (isWider) width else height
        val imagePixelsMoved = dragPixels * windowPixels / framePixels
        return (focus - imagePixelsMoved / slack).coerceIn(0f, 1f)
    }
}
