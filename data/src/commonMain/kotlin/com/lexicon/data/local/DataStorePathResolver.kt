package com.lexicon.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

internal const val SETTINGS_STORE_NAME = "lexicon_settings"
internal const val VOCABULARY_SYNC_STORE_NAME = "lexicon_vocabulary_sync"

/**
 * Where a preferences file lives, which is the one platform-specific part.
 *
 * The Android actual has to land on exactly the path the old
 * `preferencesDataStore(name = ...)` delegate used, or an upgrading install
 * reads as "my settings reset" — so it asks androidx for that path rather than
 * rebuilding it by hand.
 */
expect class DataStorePathResolver {
    /** Absolute path of the backing file for [name], suffix included. */
    fun path(name: String): String
}

fun DataStorePathResolver.createDataStore(name: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { path(name).toPath() })
