package com.lexicon.data.local

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile

actual class DataStorePathResolver(
    private val context: Context,
) {
    actual fun path(name: String): String = context.preferencesDataStoreFile(name).absolutePath
}
