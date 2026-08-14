package com.lexicon.domain.settings

import com.lexicon.boundary.SettingsRepository
import com.lexicon.interactors.settings.AppSettings
import com.lexicon.interactors.settings.ObserveSettingsUseCase
import com.lexicon.interactors.settings.ThemeMode
import com.lexicon.interactors.settings.UpdateStepCountUseCase
import com.lexicon.interactors.settings.UpdateThemeModeUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveSettingsUseCaseImpl(
    private val settingsRepository: SettingsRepository,
) : ObserveSettingsUseCase {
    override fun invoke(): Flow<AppSettings> = settingsRepository.observeSettings().map { it.toAppSettings() }
}

class UpdateThemeModeUseCaseImpl(
    private val settingsRepository: SettingsRepository,
) : UpdateThemeModeUseCase {
    override suspend fun invoke(themeMode: ThemeMode) = settingsRepository.setThemeMode(themeMode.toBoundary())
}

class UpdateStepCountUseCaseImpl(
    private val settingsRepository: SettingsRepository,
) : UpdateStepCountUseCase {
    override suspend fun invoke(stepCount: Int) =
        settingsRepository.setStepCount(
            stepCount.coerceIn(AppSettings.MIN_STEP_COUNT, AppSettings.MAX_STEP_COUNT),
        )
}
