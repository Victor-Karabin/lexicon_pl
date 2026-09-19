package com.lexicon.presentation.presets

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.core.net.toUri
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.LexiconShapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

private const val TAG = "ImageCropDialog"

private const val MAX_DECODED_SIDE = 2048

private const val JPEG_QUALITY = 90

private const val CENTRE = 0.5f

@Composable
fun ImageCropDialog(
    picked: String,
    onCropped: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val file = remember(picked) { Uri.parse(picked).path?.let(::File) }

    var bitmap by remember(picked) { mutableStateOf<Bitmap?>(null) }
    var focus by remember(picked) { mutableFloatStateOf(CENTRE) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(picked) {
        val decoded = file?.let { withContext(Dispatchers.IO) { decodeUpright(it) } }
        when {
            decoded == null -> onCropped(picked)
            !CropWindow(decoded.width, decoded.height).needsPositioning -> onCropped(picked)
            else -> bitmap = decoded
        }
    }

    val shown = bitmap ?: return
    val window = remember(shown) { CropWindow(shown.width, shown.height) }
    val image = remember(shown) { shown.asImageBitmap() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_word_image_position)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.create_word_image_position_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(CARD_IMAGE_ASPECT)
                        .clip(LexiconShapes.small),
                    contentAlignment = Alignment.Center,
                ) {
                    CropPreview(
                        image = image,
                        window = window,
                        focus = focus,
                        onDrag = { drag, frame -> focus = window.focusAfterDrag(focus, drag, frame) },
                    )
                    if (isSaving) CircularProgressIndicator()
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving && file != null,
                onClick = {
                    val source = file ?: return@TextButton
                    isSaving = true
                    scope.launch {
                        val cropped = withContext(Dispatchers.IO) { writeCrop(shown, window.rectAt(focus), source) }
                        onCropped(cropped ?: picked)
                    }
                },
            ) { Text(stringResource(R.string.create_word_image_use)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun CropPreview(
    image: ImageBitmap,
    window: CropWindow,
    focus: Float,
    onDrag: (dragPixels: Float, framePixels: Float) -> Unit,
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(CARD_IMAGE_ASPECT)
            .pointerInput(window) {
                detectDragGestures { change, drag ->
                    change.consume()
                    if (window.movesHorizontally) {
                        onDrag(drag.x, size.width.toFloat())
                    } else {
                        onDrag(drag.y, size.height.toFloat())
                    }
                }
            },
    ) {
        val rect = window.rectAt(focus)
        drawImage(
            image = image,
            srcOffset = IntOffset(rect.left, rect.top),
            srcSize = IntSize(rect.width, rect.height),
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
        )
    }
}

private fun decodeUpright(file: File): Bitmap? =
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
                val side = max(info.size.width, info.size.height)
                if (side > MAX_DECODED_SIDE) {
                    decoder.setTargetSize(
                        info.size.width * MAX_DECODED_SIDE / side,
                        info.size.height * MAX_DECODED_SIDE / side,
                    )
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_DECODED_SIDE) sample *= 2
            BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }.onFailure { Log.w(TAG, "The picked picture could not be decoded, so it is kept uncropped", it) }
        .getOrNull()

private fun writeCrop(
    bitmap: Bitmap,
    rect: CropRect,
    source: File,
): String? =
    runCatching {
        val cropped = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width, rect.height)
        val target = File(source.parentFile, "${source.nameWithoutExtension}-cropped.jpg")
        target.outputStream().use { cropped.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        source.delete()
        target.toUri().toString()
    }.onFailure { Log.w(TAG, "The crop could not be written, so the picture is kept whole", it) }
        .getOrNull()
