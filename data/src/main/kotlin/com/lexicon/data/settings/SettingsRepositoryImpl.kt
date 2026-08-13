package com.lexicon.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lexicon.boundary.AppSettingsBoundary
import com.lexicon.boundary.SettingsRepository
import com.lexicon.boundary.ThemeModeBoundary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val DEFAULT_STEP_COUNT = 10

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "lexicon_settings")

private object Keys {
    val ThemeMode = stringPreferencesKey("theme_mode")
    val StepCount = intPreferencesKey("step_count")
}

class SettingsRepositoryImpl(
    private val context: Context,
) : SettingsRepository {
    override fun observeSettings(): Flow<AppSettingsBoundary> = context.settingsDataStore.data.map { it.toSettings() }

    override suspend fun getSettings(): AppSettingsBoundary = observeSettings().first()

    override suspend fun setThemeMode(themeMode: ThemeModeBoundary) {
        context.settingsDataStore.edit { it[Keys.ThemeMode] = themeMode.name }
    }

    override suspend fun setStepCount(stepCount: Int) {
        context.settingsDataStore.edit { it[Keys.StepCount] = stepCount }
    }

    private fun Preferences.toSettings(): AppSettingsBoundary =
        AppSettingsBoundary(
            themeMode = this[Keys.ThemeMode]
                ?.let { stored -> ThemeModeBoundary.entries.firstOrNull { it.name == stored } }
                ?: ThemeModeBoundary.SYSTEM,
            stepCount = this[Keys.StepCount] ?: DEFAULT_STEP_COUNT,
        )
}
