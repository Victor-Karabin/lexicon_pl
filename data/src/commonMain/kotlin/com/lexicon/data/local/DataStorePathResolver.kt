package com.lexicon.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

internal const val SETTINGS_STORE_NAME = "lexicon_settings"
internal const val VOCABULARY_SYNC_STORE_NAME = "lexicon_vocabulary_sync"

expect class DataStorePathResolver {
    fun path(name: String): String
}

fun DataStorePathResolver.createDataStore(name: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { path(name).toPath() })
