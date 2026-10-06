package com.lexicon.presentation.common

import android.util.Log
import java.io.File

private const val TAG = "Recording"

fun deleteRecording(path: String?) {
    val file = path?.let(::File)?.takeIf { it.exists() } ?: return
    if (!file.delete()) Log.w(TAG, "A pronunciation recording could not be deleted")
}
