package com.lexicon.presentation.presets

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Where the learner's own pictures live, inside the app's own storage. */
private const val OWN_IMAGE_DIR = "word_images"

/** Matches the authority declared for the provider in the manifest. */
private const val PROVIDER_SUFFIX = ".images"

/**
 * The two ways a learner can supply a picture themselves.
 *
 * Both hand back a `file://` string, the same shape of thing as a searched picture's
 * URL, so nothing downstream has to know where a picture came from.
 */
@Immutable
class OwnImagePicker internal constructor(
    private val fromLibrary: () -> Unit,
    private val fromCamera: () -> Unit,
) {
    fun pickFromLibrary() = fromLibrary()

    fun takePhoto() = fromCamera()
}

/**
 * Wires up the photo picker and the camera for the screen that calls it.
 *
 * A picked picture is copied into the app's files rather than kept as the URI it
 * arrived as: that URI is readable only while this screen is up, and the word is meant
 * to keep its picture. A camera shot is written into the same place to begin with, so
 * it needs no copy — only somewhere the camera app is allowed to write, which is what
 * the FileProvider is for.
 */
@Composable
fun rememberOwnImagePicker(onPicked: (String) -> Unit): OwnImagePicker {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Where the camera has been told to put the next shot. Held across the launch
    // because the result says only whether it was taken, not where it went.
    var pendingPhoto by remember { mutableStateOf<File?>(null) }

    val library = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            withContext(Dispatchers.IO) { context.copyIntoOwnImages(uri) }?.let { onPicked(it.toUri().toString()) }
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val file = pendingPhoto
        pendingPhoto = null
        when {
            file == null -> Unit
            // Cancelled shots leave a zero-length file behind, which would show as a
            // broken tile in the row for as long as the app is installed.
            taken -> onPicked(file.toUri().toString())
            else -> file.delete()
        }
    }

    return remember(context) {
        OwnImagePicker(
            fromLibrary = {
                library.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            fromCamera = {
                val file = context.newOwnImageFile()
                pendingPhoto = file
                camera.launch(
                    FileProvider.getUriForFile(context, context.packageName + PROVIDER_SUFFIX, file),
                )
            },
        )
    }
}

private fun Context.ownImageDir(): File = File(filesDir, OWN_IMAGE_DIR).apply { mkdirs() }

private fun Context.newOwnImageFile(): File = File(ownImageDir(), "${UUID.randomUUID()}.jpg")

/** Null when the picture cannot be read — a revoked permission, or a file that moved. */
private fun Context.copyIntoOwnImages(uri: Uri): File? {
    val file = newOwnImageFile()
    return runCatching {
        contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use(input::copyTo)
        } ?: return null
        file
    }.getOrElse {
        file.delete()
        null
    }
}
