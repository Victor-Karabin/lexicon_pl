package com.lexicon.interactors.settings

import kotlinx.coroutines.flow.Flow

interface ObserveSettingsUseCase {
    operator fun invoke(): Flow<AppSettings>
}

interface UpdateThemeModeUseCase {
    suspend operator fun invoke(themeMode: ThemeMode)
}

interface UpdateStepCountUseCase {
    suspend operator fun invoke(stepCount: Int)
}
