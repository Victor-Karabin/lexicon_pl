package com.lexicon.domain.settings

import com.lexicon.boundary.SettingsRepository
import com.lexicon.interactors.settings.AppSettings
import com.lexicon.interactors.settings.ObserveSettingsUseCase
import com.lexicon.interactors.settings.ThemeMode
import com.lexicon.interactors.settings.UpdateStepCountUseCase
import com.lexicon.interactors.settings.UpdateThemeModeUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveSettingsUseCaseImpl
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
    ) : ObserveSettingsUseCase {
        override fun invoke(): Flow<AppSettings> = settingsRepository.observeSettings().map { it.toAppSettings() }
    }

class UpdateThemeModeUseCaseImpl
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
    ) : UpdateThemeModeUseCase {
        override suspend fun invoke(themeMode: ThemeMode) = settingsRepository.setThemeMode(themeMode.toBoundary())
    }

class UpdateStepCountUseCaseImpl
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
    ) : UpdateStepCountUseCase {
        // Clamped here rather than in the UI so any caller gets a storable value.
        override suspend fun invoke(stepCount: Int) =
            settingsRepository.setStepCount(
                stepCount.coerceIn(AppSettings.MIN_STEP_COUNT, AppSettings.MAX_STEP_COUNT),
            )
    }
