package com.lexicon.presentation.presets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.presets.DeleteWordUseCase
import com.lexicon.interactors.presets.GetPresetVocabularyUseCase
import com.lexicon.interactors.presets.GetVocabularyPresetUseCase
import com.lexicon.interactors.presets.GetWordPresetMembershipsUseCase
import com.lexicon.interactors.presets.ObserveWordStatusesUseCase
import com.lexicon.interactors.presets.RestoreWordUseCase
import com.lexicon.interactors.presets.SetWordPresetMembershipUseCase
import com.lexicon.interactors.presets.SetWordStatusUseCase
import com.lexicon.model.vocabulary.PresetId
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.VocabularyPreset
import com.lexicon.model.vocabulary.Word
import com.lexicon.model.vocabulary.WordStatus
import com.lexicon.model.vocabulary.statusOf
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

sealed interface PresetDetailUiState {
    data object Loading : PresetDetailUiState

    data object NotFound : PresetDetailUiState

    data class Loaded(
        val preset: VocabularyPreset,
        val words: ImmutableList<Word> = persistentListOf(),
        val wordStatuses: Map<VocabularyId, WordStatus> = emptyMap(),
        val languageTag: String = "en",
        val isLoadingWords: Boolean = true,
        val lastDeleted: DeletedItem? = null,
    ) : PresetDetailUiState
}

const val PRESET_ID_ARG = "presetId"

class PresetDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val getPreset: GetVocabularyPresetUseCase,
    private val getPresetVocabulary: GetPresetVocabularyUseCase,
    private val setWordStatus: SetWordStatusUseCase,
    private val deleteWord: DeleteWordUseCase,
    private val restoreWord: RestoreWordUseCase,
    observeWordStatuses: ObserveWordStatusesUseCase,
    getWordPresetMemberships: GetWordPresetMembershipsUseCase,
    setWordPresetMembership: SetWordPresetMembershipUseCase,
    private val dispatchers: DispatcherProvider,
    private val speechSynthesizer: SpeechSynthesizer,
) : ViewModel() {
    private val presetId = PresetId(savedStateHandle.get<String>(PRESET_ID_ARG).orEmpty())

    private data class Content(
        val preset: VocabularyPreset?,
        val words: List<Word>,
        val wordsLoaded: Boolean = false,
        val lastDeleted: DeletedItem? = null,
    )

    private val content = MutableStateFlow<Content?>(null)

    private val changePresets =
        ChangePresetsController(
            scope = viewModelScope,
            ioContext = dispatchers.io,
            getMemberships = getWordPresetMemberships,
            setMembership = setWordPresetMembership,
            onChanged = { refreshWords() },
        )
    val changePresetsState = changePresets.state

    fun onChangePresetsRequested(word: Word) {
        val languageTag = (uiState.value as? PresetDetailUiState.Loaded)?.languageTag ?: return
        changePresets.open(word, languageTag)
    }

    fun onChangePresetsDismissed() = changePresets.dismiss()

    fun onPresetMembershipToggled(
        presetId: PresetId,
        isMember: Boolean,
    ) = changePresets.toggle(presetId, isMember)

    val uiState: StateFlow<PresetDetailUiState> =
        combine(content, observeWordStatuses()) { loaded, statuses ->
            when {
                loaded == null -> PresetDetailUiState.Loading
                loaded.preset == null -> PresetDetailUiState.NotFound
                else ->
                    PresetDetailUiState.Loaded(
                        preset = loaded.preset,
                        words = loaded.words
                            .toImmutableList(),
                        wordStatuses = statuses,
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
            content.value = Content(preset = preset, words = emptyList())
            content.value = Content(
                preset = preset,
                words = getPresetVocabulary(presetId).sortedForDisplay(),
                wordsLoaded = true,
            )
        }
    }

    fun onWordDeleted(word: Word) {
        viewModelScope.launch(dispatchers.io) {
            deleteWord(word.id)
            val refreshed = getPreset(presetId)
            content.update { current ->
                current?.copy(
                    preset = refreshed ?: current.preset,
                    words = current.words.filterNot { it.id == word.id },
                    lastDeleted = DeletedItem.Word(word.id, word.text),
                )
            }
        }
    }

    fun onSelectedWordsDeleted(ids: Set<VocabularyId>) {
        if (ids.isEmpty()) return
        viewModelScope.launch(dispatchers.io) {
            ids.forEach { deleteWord(it) }
            val refreshed = getPreset(presetId)
            content.update { current ->
                current?.copy(
                    preset = refreshed ?: current.preset,
                    words = current.words.filterNot { it.id in ids },
                    lastDeleted = DeletedItem.Words(ids.toList()),
                )
            }
        }
    }

    fun onUndoDelete() {
        val deleted = (content.value?.lastDeleted as? DeletedItem.Word) ?: return
        viewModelScope.launch(dispatchers.io) {
            restoreWord(deleted.id)
            val refreshed = getPreset(presetId)
            content.update { current ->
                current?.copy(
                    preset = refreshed ?: current.preset,
                    words = getPresetVocabulary(presetId).sortedForDisplay(),
                    lastDeleted = null,
                )
            }
        }
    }

    fun onDeleteMessageShown() = content.update { it?.copy(lastDeleted = null) }

    private suspend fun refreshWords() {
        val refreshed = getPreset(presetId)
        val words = getPresetVocabulary(presetId).sortedForDisplay()
        content.update { current ->
            current?.copy(preset = refreshed ?: current.preset, words = words)
        }
    }

    fun onPronounceWord(word: Word) {
        viewModelScope.launch(dispatchers.io) {
            runCatching { speechSynthesizer.speak(word.text) }
        }
    }

    fun onWordStatusCycled(id: VocabularyId) {
        val loaded = uiState.value as? PresetDetailUiState.Loaded ?: return
        val current = loaded.wordStatuses.statusOf(id)

        viewModelScope.launch(dispatchers.io) { setWordStatus(id, current.next()) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

private val polishCollator: Collator = Collator.getInstance(Locale.forLanguageTag("pl"))

internal fun List<Word>.sortedForDisplay(): List<Word> = sortedWith(compareBy(polishCollator) { it.text })
