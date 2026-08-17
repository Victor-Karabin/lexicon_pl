package com.lexicon.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.android.SpeechSynthesizer
import com.lexicon.android.SpeechVoice
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.settings.AppSettings
import com.lexicon.interactors.settings.ObserveSettingsUseCase
import com.lexicon.interactors.settings.ThemeMode
import com.lexicon.interactors.settings.UpdateStepCountUseCase
import com.lexicon.interactors.settings.UpdateThemeModeUseCase
import com.lexicon.interactors.settings.UpdateVoiceUseCase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    observeSettings: ObserveSettingsUseCase,
    private val updateThemeMode: UpdateThemeModeUseCase,
    private val updateStepCount: UpdateStepCountUseCase,
    private val updateVoice: UpdateVoiceUseCase,
    private val speechSynthesizer: SpeechSynthesizer,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    val uiState: StateFlow<AppSettings> =
        observeSettings().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = AppSettings.Default,
        )

    private val _voices = MutableStateFlow<ImmutableList<SpeechVoice>>(persistentListOf())
    val voices: StateFlow<ImmutableList<SpeechVoice>> = _voices.asStateFlow()

    init {
        viewModelScope.launch(dispatchers.io) {
            _voices.value = speechSynthesizer.voices().toImmutableList()
        }
    }

    /**
     * Chooses a voice and says something in it, because the platform tells us nothing
     * about how a voice sounds — not even whether it is a man or a woman — so the only
     * way to pick one is to hear it.
     */
    fun onVoiceSelected(voice: SpeechVoice) {
        viewModelScope.launch(dispatchers.io) {
            updateVoice(voice.id)
            runCatching { speechSynthesizer.speak(SAMPLE) }
        }
    }

    fun onThemeModeSelected(themeMode: ThemeMode) {
        viewModelScope.launch(dispatchers.io) { updateThemeMode(themeMode) }
    }

    fun onStepCountChanged(stepCount: Int) {
        viewModelScope.launch(dispatchers.io) { updateStepCount(stepCount) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L

        const val SAMPLE = "Dzień dobry, zaczynamy naukę."
    }
}
