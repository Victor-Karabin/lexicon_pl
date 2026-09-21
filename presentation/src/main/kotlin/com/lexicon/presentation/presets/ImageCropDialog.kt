package com.lexicon.presentation.presets

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import coil.imageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.lexicon.common.CARD_IMAGE_ASPECT
import com.lexicon.common.CropRect
import com.lexicon.common.CropWindow
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.LexiconShapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "ImageCropDialog"

private const val MAX_DECODED_SIDE = 2048

private const val JPEG_QUALITY = 90

private const val CENTRE = 0.5f

@Composable
fun ImageCropDialog(
    picked: String,
    onCropped: (String) -> Unit,
    onDismiss: () -> Unit,
    alwaysAsk: Boolean = false,
    replacesPicked: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var bitmap by remember(picked) { mutableStateOf<Bitmap?>(null) }
    var focus by remember(picked) { mutableFloatStateOf(CENTRE) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(picked) {
        val loaded = context.loadUpright(picked)
        when {
            loaded == null -> if (alwaysAsk) onDismiss() else onCropped(picked)
            !alwaysAsk && !CropWindow(loaded.width, loaded.height).needsPositioning -> onCropped(picked)
            else -> bitmap = loaded
        }
    }

    val shown = bitmap
    if (shown == null) {
        if (alwaysAsk) LoadingDialog(onDismiss)
        return
    }
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
                enabled = !isSaving,
                onClick = {
                    isSaving = true
                    scope.launch {
                        val source = Uri.parse(picked).takeIf { replacesPicked && it.scheme == "file" }?.path?.let(::File)
                        val cropped = withContext(Dispatchers.IO) {
                            context.writeCrop(shown, window.rectAt(focus))?.also { source?.delete() }
                        }
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

@Composable
private fun LoadingDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_word_image_position)) },
        text = {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(CARD_IMAGE_ASPECT), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private suspend fun Context.loadUpright(url: String): Bitmap? {
    val request = ImageRequest.Builder(this)
        .data(url)
        .size(MAX_DECODED_SIDE)
        .allowHardware(false)
        .build()
    return when (val result = imageLoader.execute(request)) {
        is SuccessResult -> result.drawable.toBitmap()
        is ErrorResult -> {
            Log.w(TAG, "The picture could not be loaded for positioning", result.throwable)
            null
        }
    }
}

private fun Context.writeCrop(
    bitmap: Bitmap,
    rect: CropRect,
): String? =
    runCatching {
        val cropped = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width, rect.height)
        val target = newOwnImageFile()
        target.outputStream().use { cropped.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        target.toUri().toString()
    }.onFailure { Log.w(TAG, "The crop could not be written, so the picture is kept whole", it) }
        .getOrNull()
