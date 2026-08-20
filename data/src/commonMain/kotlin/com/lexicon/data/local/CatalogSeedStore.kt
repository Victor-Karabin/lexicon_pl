package com.lexicon.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val SyncedFingerprint = stringPreferencesKey("synced_asset_fingerprint")
private val SyncedPresetFingerprint = stringPreferencesKey("synced_preset_fingerprint")
private val SyncedCourseFingerprint = stringPreferencesKey("synced_course_fingerprint")
private val SyncedVerbFingerprint = stringPreferencesKey("synced_verb_fingerprint")
private val SyncedAppVersion = intPreferencesKey("synced_app_version")

class CatalogSeedStore(
    private val dataStore: DataStore<Preferences>,
) {
    suspend fun syncedFingerprint(): String? = dataStore.data.map { it[SyncedFingerprint] }.first()

    suspend fun setSyncedFingerprint(fingerprint: String) {
        dataStore.edit { it[SyncedFingerprint] = fingerprint }
    }

    suspend fun syncedPresetFingerprint(): String? = dataStore.data.map { it[SyncedPresetFingerprint] }.first()

    suspend fun setSyncedPresetFingerprint(fingerprint: String) {
        dataStore.edit { it[SyncedPresetFingerprint] = fingerprint }
    }

    suspend fun syncedCourseFingerprint(): String? = dataStore.data.map { it[SyncedCourseFingerprint] }.first()

    suspend fun setSyncedCourseFingerprint(fingerprint: String) {
        dataStore.edit { it[SyncedCourseFingerprint] = fingerprint }
    }

    suspend fun syncedVerbFingerprint(): String? = dataStore.data.map { it[SyncedVerbFingerprint] }.first()

    suspend fun setSyncedVerbFingerprint(fingerprint: String) {
        dataStore.edit { it[SyncedVerbFingerprint] = fingerprint }
    }

    suspend fun syncedAppVersion(): Int? = dataStore.data.map { it[SyncedAppVersion] }.first()

    suspend fun setSyncedAppVersion(version: Int) {
        dataStore.edit { it[SyncedAppVersion] = version }
    }
}
