package com.lexicon.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.vocabularySyncDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "lexicon_vocabulary_sync")

private val SyncedFingerprint = stringPreferencesKey("synced_asset_fingerprint")
private val SyncedPresetFingerprint = stringPreferencesKey("synced_preset_fingerprint")
private val SyncedCourseFingerprint = stringPreferencesKey("synced_course_fingerprint")

class VocabularySyncStore(
    private val context: Context,
) {
    suspend fun syncedFingerprint(): String? = context.vocabularySyncDataStore.data.map { it[SyncedFingerprint] }.first()

    suspend fun setSyncedFingerprint(fingerprint: String) {
        context.vocabularySyncDataStore.edit { it[SyncedFingerprint] = fingerprint }
    }

    suspend fun syncedPresetFingerprint(): String? = context.vocabularySyncDataStore.data.map { it[SyncedPresetFingerprint] }.first()

    suspend fun setSyncedPresetFingerprint(fingerprint: String) {
        context.vocabularySyncDataStore.edit { it[SyncedPresetFingerprint] = fingerprint }
    }

    suspend fun syncedCourseFingerprint(): String? = context.vocabularySyncDataStore.data.map { it[SyncedCourseFingerprint] }.first()

    suspend fun setSyncedCourseFingerprint(fingerprint: String) {
        context.vocabularySyncDataStore.edit { it[SyncedCourseFingerprint] = fingerprint }
    }
}
