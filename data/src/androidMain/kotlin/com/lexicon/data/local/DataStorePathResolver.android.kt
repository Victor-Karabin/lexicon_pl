package com.lexicon.data.local

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile

actual class DataStorePathResolver(
    private val context: Context,
) {
    // preferencesDataStoreFile is what the preferencesDataStore delegate used
    // internally, so an install that was writing filesDir/datastore/<name>.preferences_pb
    // before this became multiplatform keeps reading the same file.
    actual fun path(name: String): String = context.preferencesDataStoreFile(name).absolutePath
}
