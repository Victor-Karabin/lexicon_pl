package com.lexicon.domain.settings

import com.lexicon.boundary.AppSettingsBoundary
import com.lexicon.boundary.ThemeModeBoundary
import com.lexicon.interactors.settings.AppSettings
import com.lexicon.interactors.settings.ThemeMode

fun AppSettingsBoundary.toAppSettings(): AppSettings = AppSettings(themeMode = themeMode.toThemeMode(), stepCount = stepCount)

fun ThemeModeBoundary.toThemeMode(): ThemeMode =
    when (this) {
        ThemeModeBoundary.SYSTEM -> ThemeMode.SYSTEM
        ThemeModeBoundary.LIGHT -> ThemeMode.LIGHT
        ThemeModeBoundary.DARK -> ThemeMode.DARK
    }

fun ThemeMode.toBoundary(): ThemeModeBoundary =
    when (this) {
        ThemeMode.SYSTEM -> ThemeModeBoundary.SYSTEM
        ThemeMode.LIGHT -> ThemeModeBoundary.LIGHT
        ThemeMode.DARK -> ThemeModeBoundary.DARK
    }
