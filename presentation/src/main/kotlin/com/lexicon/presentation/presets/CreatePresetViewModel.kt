package com.lexicon.presentation.presets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.interactors.presets.CreatePresetUseCase
import com.lexicon.interactors.presets.PresetDraftException
import com.lexicon.interactors.presets.PresetDraftProblem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreatePresetUiState(
    val name: String = "",
    val description: String = "",
    val icon: String = DEFAULT_PRESET_ICON,
    val color: String = DEFAULT_PRESET_COLOR,
    val isSaving: Boolean = false,
    val problem: PresetDraftProblem? = null,
    /** Set once the preset is stored; the screen closes on it. */
    val savedName: String? = null,
) {
    val canSave: Boolean get() = name.isNotBlank() && !isSaving
}

class CreatePresetViewModel(
    private val createPreset: CreatePresetUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreatePresetUiState())
    val uiState: StateFlow<CreatePresetUiState> = _uiState.asStateFlow()

    // Clearing the problem on edit: the complaint was about what was typed before.
    fun onNameChanged(name: String) = _uiState.update { it.copy(name = name, problem = null) }

    fun onDescriptionChanged(description: String) = _uiState.update { it.copy(description = description) }

    fun onIconSelected(icon: String) = _uiState.update { it.copy(icon = icon) }

    fun onColorSelected(color: String) = _uiState.update { it.copy(color = color) }

    fun onSave() {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true, problem = null) }

        viewModelScope.launch {
            createPreset(
                title = state.name,
                description = state.description,
                icon = state.icon,
                color = state.color,
            ).fold(
                onSuccess = { preset -> _uiState.update { it.copy(isSaving = false, savedName = state.name.trim()) } },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            problem = (error as? PresetDraftException)?.problem ?: PresetDraftProblem.MISSING_TITLE,
                        )
                    }
                },
            )
        }
    }
}
