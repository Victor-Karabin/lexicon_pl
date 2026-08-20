package com.lexicon.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lexicon.boundary.AppSettingsBoundary
import com.lexicon.boundary.SettingsRepository
import com.lexicon.boundary.ThemeModeBoundary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val DEFAULT_STEP_COUNT = 10

private object Keys {
    val ThemeMode = stringPreferencesKey("theme_mode")
    val StepCount = intPreferencesKey("step_count")
    val VoiceId = stringPreferencesKey("voice_id")
}

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override fun observeSettings(): Flow<AppSettingsBoundary> = dataStore.data.map { it.toSettings() }

    override suspend fun getSettings(): AppSettingsBoundary = observeSettings().first()

    override suspend fun setThemeMode(themeMode: ThemeModeBoundary) {
        dataStore.edit { it[Keys.ThemeMode] = themeMode.name }
    }

    override suspend fun setStepCount(stepCount: Int) {
        dataStore.edit { it[Keys.StepCount] = stepCount }
    }

    override suspend fun setVoiceId(voiceId: String?) {
        dataStore.edit { preferences ->
            if (voiceId == null) preferences.remove(Keys.VoiceId) else preferences[Keys.VoiceId] = voiceId
        }
    }

    private fun Preferences.toSettings(): AppSettingsBoundary =
        AppSettingsBoundary(
            themeMode = this[Keys.ThemeMode]
                ?.let { stored -> ThemeModeBoundary.entries.firstOrNull { it.name == stored } }
                ?: ThemeModeBoundary.SYSTEM,
            stepCount = this[Keys.StepCount] ?: DEFAULT_STEP_COUNT,
            voiceId = this[Keys.VoiceId],
        )
}
