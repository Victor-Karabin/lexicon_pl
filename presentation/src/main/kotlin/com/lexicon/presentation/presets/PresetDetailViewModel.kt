package com.lexicon.presentation.presets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.presets.DeleteWordUseCase
import com.lexicon.interactors.presets.GetPresetVocabularyUseCase
import com.lexicon.interactors.presets.GetVocabularyPresetUseCase
import com.lexicon.interactors.presets.ObserveFavouriteWordIdsUseCase
import com.lexicon.interactors.presets.PresetFavouriteState
import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.PresetWord
import com.lexicon.interactors.presets.RestoreWordUseCase
import com.lexicon.interactors.presets.SetPresetFavouriteUseCase
import com.lexicon.interactors.presets.ToggleWordFavouriteUseCase
import com.lexicon.interactors.presets.VocabularyId
import com.lexicon.interactors.presets.VocabularyPreset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale
import javax.inject.Inject

sealed interface PresetDetailUiState {
    data object Loading : PresetDetailUiState

    /** A preset id can outlive the preset it names — saved state, or a catalogue that changed. */
    data object NotFound : PresetDetailUiState

    data class Loaded(
        val preset: VocabularyPreset,
        val words: ImmutableList<PresetWord> = persistentListOf(),
        val favouriteState: PresetFavouriteState = PresetFavouriteState.NONE,
        val languageTag: String = "en",
        /**
         * Tracked, not inferred from an empty list: a preset whose every word has been deleted
         * is legitimately empty, and inferring would leave it spinning for words that are never
         * coming.
         */
        val isLoadingWords: Boolean = true,
        /** The last deletion, kept so it can be undone; see [PresetDetailViewModel]. */
        val lastDeleted: DeletedItem? = null,
    ) : PresetDetailUiState
}

const val PRESET_ID_ARG = "presetId"

@HiltViewModel
class PresetDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val getPreset: GetVocabularyPresetUseCase,
        private val getPresetVocabulary: GetPresetVocabularyUseCase,
        private val toggleWordFavourite: ToggleWordFavouriteUseCase,
        private val deleteWord: DeleteWordUseCase,
        private val restoreWord: RestoreWordUseCase,
        private val setPresetFavourite: SetPresetFavouriteUseCase,
        observeFavouriteWordIds: ObserveFavouriteWordIdsUseCase,
        private val dispatchers: DispatcherProvider,
    ) : ViewModel() {
        private val presetId = PresetId(savedStateHandle.get<String>(PRESET_ID_ARG).orEmpty())

        /** What was loaded once; favourite state is layered on top of it as it changes. */
        private data class Content(
            val preset: VocabularyPreset?,
            val words: List<PresetWord>,
            val wordsLoaded: Boolean = false,
            val lastDeleted: DeletedItem? = null,
        )

        private val content = MutableStateFlow<Content?>(null)

        val uiState: StateFlow<PresetDetailUiState> =
            combine(content, observeFavouriteWordIds()) { loaded, favourites ->
                when {
                    loaded == null -> PresetDetailUiState.Loading
                    loaded.preset == null -> PresetDetailUiState.NotFound
                    else ->
                        PresetDetailUiState.Loaded(
                            preset = loaded.preset,
                            // Re-derived rather than stored, so a heart tapped here or on the
                            // browser is reflected without reloading the word list.
                            words = loaded.words
                                .map { it.copy(isFavourite = it.id in favourites) }
                                .toImmutableList(),
                            favouriteState = favouriteStateOf(loaded.preset, favourites),
                            isLoadingWords = !loaded.wordsLoaded,
                            lastDeleted = loaded.lastDeleted,
                        )
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = PresetDetailUiState.Loading,
            )

        init {
            viewModelScope.launch(dispatchers.io) {
                val preset = getPreset(presetId)
                if (preset == null) {
                    content.value = Content(preset = null, words = emptyList())
                    return@launch
                }
                // The header shows first: resolving a thousand ids takes a query, and a header
                // that appears at once reads better than a screen that stays blank.
                content.value = Content(preset = preset, words = emptyList())
                content.value = Content(
                    preset = preset,
                    words = getPresetVocabulary(presetId).sortedForDisplay(),
                    wordsLoaded = true,
                )
            }
        }

        /**
         * Deletes at once, as the button says, and keeps enough to put it back. The row leaves
         * the list immediately rather than waiting for a reload: it is under the user's finger.
         */
        fun onWordDeleted(word: PresetWord) {
            viewModelScope.launch(dispatchers.io) {
                deleteWord(word.id)
                content.update { current ->
                    current?.copy(
                        words = current.words.filterNot { it.id == word.id },
                        lastDeleted = DeletedItem.Word(word.id, word.text),
                    )
                }
            }
        }

        fun onUndoDelete() {
            val deleted = (content.value?.lastDeleted as? DeletedItem.Word) ?: return
            viewModelScope.launch(dispatchers.io) {
                restoreWord(deleted.id)
                content.update { current ->
                    current?.copy(
                        words = getPresetVocabulary(presetId).sortedForDisplay(),
                        lastDeleted = null,
                    )
                }
            }
        }

        /** Cleared once shown, so the same message cannot be offered twice. */
        fun onDeleteMessageShown() = content.update { it?.copy(lastDeleted = null) }

        fun onWordFavouriteToggled(
            id: VocabularyId,
            isFavourite: Boolean,
        ) {
            viewModelScope.launch(dispatchers.io) { toggleWordFavourite(id, isFavourite) }
        }

        /** Partly-favourited counts as off, so one tap completes the preset rather than clearing it. */
        fun onPresetFavouriteToggled(current: PresetFavouriteState) {
            viewModelScope.launch(dispatchers.io) {
                setPresetFavourite(presetId, current != PresetFavouriteState.ALL)
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }

internal fun favouriteStateOf(
    preset: VocabularyPreset,
    favourites: Set<VocabularyId>,
): PresetFavouriteState =
    when {
        preset.vocabularyIds.none { it in favourites } -> PresetFavouriteState.NONE
        preset.vocabularyIds.all { it in favourites } -> PresetFavouriteState.ALL
        else -> PresetFavouriteState.SOME
    }

/**
 * Polish collation, not code-point order: ą belongs directly after a, and ł after l. Sorting
 * by raw string would scatter every accented word to the end of the list.
 */
private val polishCollator: Collator = Collator.getInstance(Locale.forLanguageTag("pl"))

internal fun List<PresetWord>.sortedForDisplay(): List<PresetWord> = sortedWith(compareBy(polishCollator) { it.text })
