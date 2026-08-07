package com.lexicon.interactors.settings

import kotlinx.coroutines.flow.Flow

/** Emits the current settings, then again on every change. */
interface ObserveSettingsUseCase {
    operator fun invoke(): Flow<AppSettings>
}

interface UpdateThemeModeUseCase {
    suspend operator fun invoke(themeMode: ThemeMode)
}

/**
 * Persists how many steps a newly created session should contain. Values outside
 * [AppSettings.MIN_STEP_COUNT]..[AppSettings.MAX_STEP_COUNT] are clamped.
 */
interface UpdateStepCountUseCase {
    suspend operator fun invoke(stepCount: Int)
}
