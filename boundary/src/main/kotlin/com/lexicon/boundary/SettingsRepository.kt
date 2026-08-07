package com.lexicon.boundary

import kotlinx.coroutines.flow.Flow

enum class ThemeModeBoundary { SYSTEM, LIGHT, DARK }

data class AppSettingsBoundary(
    val themeMode: ThemeModeBoundary,
    /** Steps generated for a newly created training session. */
    val stepCount: Int,
)

interface SettingsRepository {
    /** Emits the current settings, then again on every change. */
    fun observeSettings(): Flow<AppSettingsBoundary>

    /** Reads the settings once, for callers that only need a snapshot (e.g. starting a session). */
    suspend fun getSettings(): AppSettingsBoundary

    suspend fun setThemeMode(themeMode: ThemeModeBoundary)

    suspend fun setStepCount(stepCount: Int)
}
