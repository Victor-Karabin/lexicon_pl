package com.lexicon.presentation.presets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.interactors.presets.CreateWordUseCase
import com.lexicon.interactors.presets.GenerateWordExampleUseCase
import com.lexicon.interactors.presets.GetPinnedImageUseCase
import com.lexicon.interactors.presets.GetVocabularyPresetsUseCase
import com.lexicon.interactors.presets.GetWordPresetMembershipsUseCase
import com.lexicon.interactors.presets.GetWordUseCase
import com.lexicon.interactors.presets.PresetMembership
import com.lexicon.interactors.presets.SearchImageCandidatesUseCase
import com.lexicon.interactors.presets.SetWordPresetUseCase
import com.lexicon.interactors.presets.TranslateWordUseCase
import com.lexicon.interactors.presets.UpdateWordUseCase
import com.lexicon.interactors.presets.WordDraftException
import com.lexicon.interactors.presets.WordDraftProblem
import com.lexicon.model.vocabulary.ExampleSentence
import com.lexicon.model.vocabulary.PresetId
import com.lexicon.model.vocabulary.VocabularyId
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

private const val TYPING_SETTLE_MS = 600L

const val WORD_ID_ARG = "wordId"

internal fun String.isOwnImage(): Boolean = startsWith("file:")

data class CreateWordUiState(
    val isEditing: Boolean = false,
    val isMissing: Boolean = false,
    val text: String = "",
    val translation: String = "",
    val memberships: ImmutableList<PresetMembership> = persistentListOf(),
    val imageCandidates: ImmutableList<String> = persistentListOf(),
    val ownImages: ImmutableList<String> = persistentListOf(),
    val selectedImage: String? = null,
    val example: String = "",
    val isWritingExample: Boolean = false,
    val exampleFailed: Boolean = false,
    val presetFailed: Boolean = false,
    val languageTag: String = "en",
    val isTranslating: Boolean = false,
    val isLoadingImages: Boolean = false,
    val hasSearchedImages: Boolean = false,
    val isSaving: Boolean = false,
    val problem: WordDraftProblem? = null,
    val savedWord: String? = null,
) {
    val canSave: Boolean get() = text.isNotBlank() && translation.isNotBlank() && !isSaving

    val isImageLoading: Boolean get() = selectedImage == null && isLoadingImages

    val sentence: ExampleSentence get() = ExampleSentence.of(example, word = text)

    val canWriteExample: Boolean get() = text.isNotBlank() && !isWritingExample
}

class CreateWordViewModel(
    savedStateHandle: SavedStateHandle,
    private val createWord: CreateWordUseCase,
    private val updateWord: UpdateWordUseCase,
    private val getWord: GetWordUseCase,
    private val translateWord: TranslateWordUseCase,
    private val searchImageCandidates: SearchImageCandidatesUseCase,
    private val getPresets: GetVocabularyPresetsUseCase,
    private val getWordPresetMemberships: GetWordPresetMembershipsUseCase,
    private val getPinnedImage: GetPinnedImageUseCase,
    private val generateExample: GenerateWordExampleUseCase,
    private val setWordPreset: SetWordPresetUseCase,
    private val speechSynthesizer: SpeechSynthesizer,
) : ViewModel() {
    private val editing: VocabularyId? =
        savedStateHandle.get<String>(WORD_ID_ARG)?.toLongOrNull()?.let(::VocabularyId)

    private val _uiState = MutableStateFlow(
        CreateWordUiState(isEditing = editing != null, isLoadingImages = editing != null),
    )
    val uiState: StateFlow<CreateWordUiState> = _uiState.asStateFlow()

    private var translateJob: Job? = null
    private var imageJob: Job? = null
    private val presetJobs = mutableMapOf<PresetId, Job>()

    private var shownImages = 0

    private var textWasFilledIn = false
    private var translationWasFilledIn = false

    init {
        viewModelScope.launch {
            if (editing != null) load(editing) else loadPresetsForNewWord()
        }
    }

    private suspend fun loadPresetsForNewWord() {
        val presets = getPresets().map { PresetMembership(preset = it, isMember = false) }
        _uiState.update { it.copy(memberships = presets.toImmutableList()) }
    }

    private suspend fun load(id: VocabularyId) {
        val word = getWord(id)
        if (word == null) {
            _uiState.update { it.copy(isMissing = true, isLoadingImages = false) }
            return
        }
        _uiState.update {
            it.copy(
                text = word.text,
                translation = word.translation,
                example = word.example,
                memberships = getWordPresetMemberships(id),
            )
        }
        loadImagesFor(word.translation, pinned = getPinnedImage(word.translation))
    }

    fun onTextChanged(text: String) {
        textWasFilledIn = false
        _uiState.update { it.copy(text = text, problem = null) }
        scheduleTranslation(from = text, toPolish = false)
    }

    fun onTranslationChanged(translation: String) {
        translationWasFilledIn = false
        _uiState.update { it.copy(translation = translation, problem = null) }
        scheduleTranslation(from = translation, toPolish = true)
        scheduleImageSearch(translation)
    }

    fun onImageSelected(url: String) = _uiState.update { it.copy(selectedImage = if (it.selectedImage == url) null else url) }

    fun onOwnImageAdded(url: String) =
        _uiState.update { state ->
            state.copy(
                ownImages = (listOf(url) + state.ownImages).distinct().toImmutableList(),
                selectedImage = url,
            )
        }

    fun onExampleChanged(example: String) = _uiState.update { it.copy(example = example, exampleFailed = false) }

    fun onExampleRequested() {
        val state = _uiState.value
        if (!state.canWriteExample) return

        _uiState.update { it.copy(isWritingExample = true, exampleFailed = false) }
        viewModelScope.launch {
            val written = generateExample(
                text = state.text,
                translation = state.translation,
                level = "",
            )
            _uiState.update {
                it.copy(
                    example = written ?: it.example,
                    isWritingExample = false,
                    exampleFailed = written == null,
                )
            }
        }
    }

    fun onExamplePlayed() {
        val sentence = _uiState.value.sentence
        if (sentence.isBlank) return
        viewModelScope.launch { runCatching { speechSynthesizer.speak(sentence.text) } }
    }

    /**
     * The chip answers at once and the write follows. A word being created has nothing
     * to attach to yet, so its memberships wait for the save; an existing one is written
     * straight away and put back the way it was if the write fails.
     */
    fun onPresetToggled(
        presetId: PresetId,
        isMember: Boolean,
    ) {
        showMembership(presetId, isMember)

        val id = editing ?: return
        presetJobs[presetId]?.cancel()
        presetJobs[presetId] = viewModelScope.launch {
            setWordPreset(wordId = id, presetId = presetId, isMember = isMember)
                .onFailure {
                    showMembership(presetId, !isMember)
                    _uiState.update { it.copy(presetFailed = true) }
                }
        }
    }

    fun onErrorShown() = _uiState.update { it.copy(presetFailed = false, exampleFailed = false) }

    private fun showMembership(
        presetId: PresetId,
        isMember: Boolean,
    ) = _uiState.update { state ->
        state.copy(
            presetFailed = false,
            memberships = state.memberships
                .map { if (it.preset.id == presetId) it.copy(isMember = isMember) else it }
                .toImmutableList(),
        )
    }

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

        val presetIds = state.memberships.filter { it.isMember }.map { it.preset.id }
        viewModelScope.launch {
            val result = if (editing != null) {
                updateWord(
                    id = editing,
                    text = state.text,
                    translation = state.translation,
                    imageUrl = state.selectedImage,
                    example = state.example,
                    presetIds = presetIds,
                )
            } else {
                createWord(
                    text = state.text,
                    translation = state.translation,
                    imageUrl = state.selectedImage,
                    example = state.example,
                    presetIds = presetIds,
                )
            }
            result.fold(
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

    private fun scheduleTranslation(
        from: String,
        toPolish: Boolean,
    ) {
        translateJob?.cancel()

        val wasFilledIn = if (toPolish) textWasFilledIn else translationWasFilledIn
        if (wasFilledIn) {
            _uiState.update { if (toPolish) it.copy(text = "") else it.copy(translation = "") }
        }
        if (from.isBlank()) return
        val target = { state: CreateWordUiState -> if (toPolish) state.text else state.translation }
        if (target(_uiState.value).isNotBlank()) return

        translateJob = viewModelScope.launch {
            delay(TYPING_SETTLE_MS)
            _uiState.update { it.copy(isTranslating = true) }
            val translated = translateWord(from, toPolish = toPolish)

            val fills = translated != null && target(_uiState.value).isBlank()

            if (fills) {
                if (toPolish) textWasFilledIn = true else translationWasFilledIn = true
            }
            _uiState.update { state ->
                when {
                    !fills -> state.copy(isTranslating = false)
                    toPolish -> state.copy(text = translated.orEmpty(), isTranslating = false)
                    else -> state.copy(translation = translated.orEmpty(), isTranslating = false)
                }
            }

            if (!toPolish) scheduleImageSearch(_uiState.value.translation)
        }
    }

    private suspend fun loadImagesFor(
        query: String,
        pinned: String? = null,
    ) {
        if (query.isBlank()) {
            _uiState.update { it.copy(isLoadingImages = false) }
            return
        }
        _uiState.update { it.copy(isLoadingImages = true) }
        val candidates = searchImageCandidates(query)
        shownImages = candidates.size
        val isOwn = pinned != null && pinned.isOwnImage()
        val withPinned = if (pinned.isNullOrBlank() || isOwn) {
            candidates
        } else {
            (listOf(pinned) + candidates).distinct().toImmutableList()
        }
        _uiState.update {
            it.copy(
                imageCandidates = withPinned,
                ownImages = if (isOwn) persistentListOf(pinned!!) else it.ownImages,
                selectedImage = pinned,
                isLoadingImages = false,
                hasSearchedImages = true,
            )
        }
    }

    private fun scheduleImageSearch(query: String) {
        imageJob?.cancel()
        shownImages = 0
        if (query.isBlank()) {
            _uiState.update {
                it.copy(
                    imageCandidates = persistentListOf(),
                    selectedImage = it.selectedImage.takeIf { url -> url in it.ownImages },
                    hasSearchedImages = false,
                )
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
                    selectedImage = it.selectedImage.takeIf { url -> url in it.ownImages },
                    isLoadingImages = false,
                    hasSearchedImages = true,
                )
            }
        }
    }
}
