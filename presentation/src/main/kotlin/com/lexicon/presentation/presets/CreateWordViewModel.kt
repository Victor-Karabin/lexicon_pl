package com.lexicon.presentation.presets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.interactors.presets.CreateWordUseCase
import com.lexicon.interactors.presets.GetVocabularyPresetsUseCase
import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.PresetMembership
import com.lexicon.interactors.presets.SearchImageCandidatesUseCase
import com.lexicon.interactors.presets.TranslateWordUseCase
import com.lexicon.interactors.presets.WordDraftException
import com.lexicon.interactors.presets.WordDraftProblem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * How long typing has to stop before the other field is filled in and pictures are
 * looked up. Both go over the network, so this is the difference between one request
 * and one per keystroke.
 */
private const val TYPING_SETTLE_MS = 600L

data class CreateWordUiState(
    val text: String = "",
    val translation: String = "",
    val memberships: ImmutableList<PresetMembership> = persistentListOf(),
    val imageCandidates: ImmutableList<String> = persistentListOf(),
    val selectedImage: String? = null,
    val languageTag: String = "en",
    val isTranslating: Boolean = false,
    val isLoadingImages: Boolean = false,
    /** True once a search has run and found nothing, as opposed to not having run. */
    val hasSearchedImages: Boolean = false,
    val isSaving: Boolean = false,
    val problem: WordDraftProblem? = null,
    val savedWord: String? = null,
) {
    val canSave: Boolean get() = text.isNotBlank() && translation.isNotBlank() && !isSaving
}

class CreateWordViewModel(
    private val createWord: CreateWordUseCase,
    private val translateWord: TranslateWordUseCase,
    private val searchImageCandidates: SearchImageCandidatesUseCase,
    private val getPresets: GetVocabularyPresetsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateWordUiState())
    val uiState: StateFlow<CreateWordUiState> = _uiState.asStateFlow()

    private var translateJob: Job? = null
    private var imageJob: Job? = null

    /** How many candidates have already been offered, so More asks past them. */
    private var shownImages = 0

    init {
        viewModelScope.launch {
            val presets = getPresets().map { PresetMembership(preset = it, isMember = false) }
            _uiState.update { it.copy(memberships = presets.toImmutableList()) }
        }
    }

    fun onTextChanged(text: String) {
        _uiState.update { it.copy(text = text, problem = null) }
        scheduleTranslation(from = text, toPolish = false)
    }

    fun onTranslationChanged(translation: String) {
        _uiState.update { it.copy(translation = translation, problem = null) }
        scheduleTranslation(from = translation, toPolish = true)
        scheduleImageSearch(translation)
    }

    fun onImageSelected(url: String) = _uiState.update { it.copy(selectedImage = if (it.selectedImage == url) null else url) }

    fun onPresetToggled(
        presetId: PresetId,
        isMember: Boolean,
    ) = _uiState.update { state ->
        state.copy(
            memberships = state.memberships
                .map { if (it.preset.id == presetId) it.copy(isMember = isMember) else it }
                .toImmutableList(),
        )
    }

    /** Asks for the next batch rather than repeating the one already on screen. */
    fun onMoreImages() {
        val query = _uiState.value.translation
        if (query.isBlank()) return
        imageJob?.cancel()
        imageJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingImages = true) }
            val more = searchImageCandidates(query, skip = shownImages)
            shownImages += more.size
            _uiState.update { state ->
                state.copy(
                    // Appended, not replaced: a picture the learner liked should not
                    // disappear because they looked at what else there was.
                    imageCandidates = (state.imageCandidates + more).distinct().toImmutableList(),
                    isLoadingImages = false,
                    hasSearchedImages = true,
                )
            }
        }
    }

    fun onSave() {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true, problem = null) }

        viewModelScope.launch {
            createWord(
                text = state.text,
                translation = state.translation,
                imageUrl = state.selectedImage,
                presetIds = state.memberships.filter { it.isMember }.map { it.preset.id },
            ).fold(
                onSuccess = { word -> _uiState.update { it.copy(isSaving = false, savedWord = word.text) } },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            problem = (error as? WordDraftException)?.problem ?: WordDraftProblem.MISSING_TEXT,
                        )
                    }
                },
            )
        }
    }

    /**
     * Fills the opposite field in once typing settles, and only while it is still
     * empty — a suggestion should never overwrite something the learner wrote, and by
     * the time a slow translation returns they may well have typed it themselves.
     */
    private fun scheduleTranslation(
        from: String,
        toPolish: Boolean,
    ) {
        translateJob?.cancel()
        if (from.isBlank()) return
        val target = { state: CreateWordUiState -> if (toPolish) state.text else state.translation }
        if (target(_uiState.value).isNotBlank()) return

        translateJob = viewModelScope.launch {
            delay(TYPING_SETTLE_MS)
            _uiState.update { it.copy(isTranslating = true) }
            val translated = translateWord(from, toPolish = toPolish)
            _uiState.update { state ->
                when {
                    translated == null -> state.copy(isTranslating = false)
                    target(state).isNotBlank() -> state.copy(isTranslating = false)
                    toPolish -> state.copy(text = translated, isTranslating = false)
                    else -> state.copy(translation = translated, isTranslating = false)
                }
            }
            // The English side may have just been filled in, which is what pictures
            // are searched by.
            if (!toPolish) scheduleImageSearch(_uiState.value.translation)
        }
    }

    private fun scheduleImageSearch(query: String) {
        imageJob?.cancel()
        shownImages = 0
        if (query.isBlank()) {
            _uiState.update {
                it.copy(imageCandidates = persistentListOf(), selectedImage = null, hasSearchedImages = false)
            }
            return
        }

        imageJob = viewModelScope.launch {
            delay(TYPING_SETTLE_MS)
            _uiState.update { it.copy(isLoadingImages = true) }
            val candidates = searchImageCandidates(query)
            shownImages = candidates.size
            _uiState.update {
                it.copy(
                    imageCandidates = candidates,
                    // The old pick belonged to a different word.
                    selectedImage = null,
                    isLoadingImages = false,
                    hasSearchedImages = true,
                )
            }
        }
    }
}
