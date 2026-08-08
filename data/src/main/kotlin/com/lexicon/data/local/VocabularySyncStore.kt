package com.lexicon.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.vocabularySyncDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "lexicon_vocabulary_sync")

private val SyncedFingerprint = stringPreferencesKey("synced_asset_fingerprint")

/**
 * Remembers which version of the bundled asset the words table was last brought in line with.
 *
 * Kept outside the database on purpose: it describes the table rather than belonging to it, and
 * a schema change that wipes the words must not also convince the app that it is up to date.
 */
@Singleton
class VocabularySyncStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        suspend fun syncedFingerprint(): String? = context.vocabularySyncDataStore.data.map { it[SyncedFingerprint] }.first()

        suspend fun setSyncedFingerprint(fingerprint: String) {
            context.vocabularySyncDataStore.edit { it[SyncedFingerprint] = fingerprint }
        }
    }
