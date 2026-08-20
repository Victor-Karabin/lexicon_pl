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

private const val OWN_IMAGE_DIR = "word_images"

private const val PROVIDER_SUFFIX = ".images"

@Immutable
class OwnImagePicker internal constructor(
    private val fromLibrary: () -> Unit,
    private val fromCamera: () -> Unit,
) {
    fun pickFromLibrary() = fromLibrary()

    fun takePhoto() = fromCamera()
}

@Composable
fun rememberOwnImagePicker(onPicked: (String) -> Unit): OwnImagePicker {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
