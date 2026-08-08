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
private val SyncedPresetFingerprint = stringPreferencesKey("synced_preset_fingerprint")

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

        suspend fun syncedPresetFingerprint(): String? = context.vocabularySyncDataStore.data.map { it[SyncedPresetFingerprint] }.first()

        suspend fun setSyncedPresetFingerprint(fingerprint: String) {
            context.vocabularySyncDataStore.edit { it[SyncedPresetFingerprint] = fingerprint }
        }
    }
