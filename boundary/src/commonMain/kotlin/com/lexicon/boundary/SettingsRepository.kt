package com.lexicon.boundary

import kotlinx.coroutines.flow.Flow

enum class ThemeModeBoundary { SYSTEM, LIGHT, DARK }

data class AppSettingsBoundary(
    val themeMode: ThemeModeBoundary,
    val stepCount: Int,
    val voiceId: String? = null,
    val passMark: Int? = null,
)

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettingsBoundary>

    suspend fun getSettings(): AppSettingsBoundary

    suspend fun setThemeMode(themeMode: ThemeModeBoundary)

    suspend fun setStepCount(stepCount: Int)

    suspend fun setVoiceId(voiceId: String?)

    suspend fun setPassMark(passMark: Int)
}
