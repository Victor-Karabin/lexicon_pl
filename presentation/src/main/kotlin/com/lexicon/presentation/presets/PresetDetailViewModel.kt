package com.lexicon.presentation.presets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.presets.GetPresetVocabularyUseCase
import com.lexicon.interactors.presets.GetVocabularyPresetUseCase
import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.PresetWord
import com.lexicon.interactors.presets.VocabularyPreset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PresetDetailUiState {
    data object Loading : PresetDetailUiState

    /** A preset id can outlive the preset it names — saved state, or a catalogue that changed. */
    data object NotFound : PresetDetailUiState

    data class Loaded(
        val preset: VocabularyPreset,
        val words: ImmutableList<PresetWord> = persistentListOf(),
        val languageTag: String = "en",
    ) : PresetDetailUiState {
        /** The preset arrives before its words do, so the list has its own loading state. */
        val isLoadingWords: Boolean get() = words.isEmpty()
    }
}

const val PRESET_ID_ARG = "presetId"

@HiltViewModel
class PresetDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val getPreset: GetVocabularyPresetUseCase,
        private val getPresetVocabulary: GetPresetVocabularyUseCase,
        private val dispatchers: DispatcherProvider,
    ) : ViewModel() {
        private val presetId = PresetId(savedStateHandle.get<String>(PRESET_ID_ARG).orEmpty())

        private val _uiState = MutableStateFlow<PresetDetailUiState>(PresetDetailUiState.Loading)
        val uiState: StateFlow<PresetDetailUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch(dispatchers.io) {
                val preset = getPreset(presetId)
                if (preset == null) {
                    _uiState.value = PresetDetailUiState.NotFound
                    return@launch
                }
                // Header first, words after: resolving a thousand ids takes a query, and a
                // header that appears at once reads better than a screen that stays blank.
                _uiState.value = PresetDetailUiState.Loaded(preset = preset)
                _uiState.value = PresetDetailUiState.Loaded(
                    preset = preset,
                    words = getPresetVocabulary(presetId),
                )
            }
        }
    }
