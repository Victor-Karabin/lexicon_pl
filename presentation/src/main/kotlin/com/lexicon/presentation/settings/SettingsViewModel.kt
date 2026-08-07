package com.lexicon.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.settings.AppSettings
import com.lexicon.interactors.settings.ObserveSettingsUseCase
import com.lexicon.interactors.settings.ThemeMode
import com.lexicon.interactors.settings.UpdateStepCountUseCase
import com.lexicon.interactors.settings.UpdateThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        observeSettings: ObserveSettingsUseCase,
        private val updateThemeMode: UpdateThemeModeUseCase,
        private val updateStepCount: UpdateStepCountUseCase,
        private val dispatchers: DispatcherProvider,
    ) : ViewModel() {
        val uiState: StateFlow<AppSettings> =
            observeSettings().stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = AppSettings.Default,
            )

        fun onThemeModeSelected(themeMode: ThemeMode) {
            viewModelScope.launch(dispatchers.io) { updateThemeMode(themeMode) }
        }

        fun onStepCountChanged(stepCount: Int) {
            viewModelScope.launch(dispatchers.io) { updateStepCount(stepCount) }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
