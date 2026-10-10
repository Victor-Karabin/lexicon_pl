package com.lexicon.presentation.conjugation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.common.runSuspendCatching
import com.lexicon.interactors.conjugation.DeleteConjugationVerbUseCase
import com.lexicon.interactors.conjugation.EnsureVerbWordUseCase
import com.lexicon.interactors.conjugation.HasDeletedVerbsUseCase
import com.lexicon.interactors.conjugation.LoadConjugationVerbsUseCase
import com.lexicon.interactors.conjugation.RestoreConjugationVerbsUseCase
import com.lexicon.interactors.conjugation.VerbConjugation
import com.lexicon.interactors.presets.FindWordsUseCase
import com.lexicon.interactors.presets.ObserveWordStatusesUseCase
import com.lexicon.interactors.presets.SetWordStatusUseCase
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import com.lexicon.model.vocabulary.WordStatus
import com.lexicon.model.vocabulary.statusOf
import com.lexicon.presentation.common.speakQuietly
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SEARCH_SETTLE_MS = 300L
private const val TAG = "VerbSelection"

data class VerbSelectionUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val verbs: ImmutableList<VerbConjugation> = persistentListOf(),
    val canRestore: Boolean = false,
    val words: ImmutableMap<String, Word> = persistentMapOf(),
    val statuses: ImmutableMap<VocabularyId, WordStatus> = persistentMapOf(),
    val pending: ImmutableMap<String, WordStatus> = persistentMapOf(),
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val nextOffset: Int = 0,
) {
    fun statusOf(verb: VerbConjugation): WordStatus =
        pending[verb.infinitive] ?: words[verb.infinitive]?.let { statuses.statusOf(it.id) } ?: WordStatus.UNDEFINED
}

class VerbSelectionViewModel(
    private val loadVerbs: LoadConjugationVerbsUseCase,
    private val deleteVerb: DeleteConjugationVerbUseCase,
    private val restoreVerbs: RestoreConjugationVerbsUseCase,
    private val hasDeletedVerbs: HasDeletedVerbsUseCase,
    private val findWords: FindWordsUseCase,
    private val ensureWord: EnsureVerbWordUseCase,
    private val setWordStatus: SetWordStatusUseCase,
    observeWordStatuses: ObserveWordStatusesUseCase,
    private val speechSynthesizer: SpeechSynthesizer,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(VerbSelectionUiState())
    val uiState: StateFlow<VerbSelectionUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var pageJob: Job? = null

    init {
        viewModelScope.launch(dispatchers.io) {
            val canRestore = hasDeletedVerbs()
            _uiState.update { it.copy(canRestore = canRestore) }
        }
        viewModelScope.launch {
            observeWordStatuses().collect { statuses -> _uiState.update { it.copy(statuses = statuses.toImmutableMap()) } }
        }
        loadFirstPage()
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }

        searchJob?.cancel()
        searchJob = viewModelScope.launch(dispatchers.io) {
            delay(SEARCH_SETTLE_MS)
            loadFirstPage()
        }
    }

    fun onMoreVerbs() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return

        _uiState.update { it.copy(isLoadingMore = true) }
        pageJob = viewModelScope.launch(dispatchers.io) {
            val more = loadVerbs.page(query = state.query, offset = state.nextOffset)
            val found = findWords(more.verbs.map { it.infinitive })
            _uiState.update {
                it.copy(
                    verbs = (it.verbs + more.verbs).distinctBy { verb -> verb.infinitive }.toImmutableList(),
                    words = (it.words + found).toImmutableMap(),
                    isLoadingMore = false,
                    nextOffset = more.nextOffset,
                    hasMore = !more.isLast,
                )
            }
        }
    }

    private fun loadFirstPage() {
        pageJob?.cancel()
        _uiState.update { it.copy(isLoading = true) }
        pageJob = viewModelScope.launch(dispatchers.io) {
            val query = _uiState.value.query
            val first = loadVerbs.page(query = query)
            val found = findWords(first.verbs.map { it.infinitive })
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    verbs = first.verbs,
                    words = (it.words + found).toImmutableMap(),
                    nextOffset = first.nextOffset,
                    hasMore = !first.isLast,
                )
            }
        }
    }

    fun onStatusCycled(verb: VerbConjugation) {
        val state = _uiState.value
        val next = state.statusOf(verb).next()
        val word = state.words[verb.infinitive]
        if (word != null) {
            viewModelScope.launch(dispatchers.io + NonCancellable) { saveStatus(word.id, next) }
            return
        }
        val isCreating = verb.infinitive in state.pending
        _uiState.update { it.copy(pending = (it.pending + (verb.infinitive to next)).toImmutableMap()) }
        if (!isCreating) viewModelScope.launch(dispatchers.io + NonCancellable) { createWordWithStatus(verb) }
    }

    fun onPronounce(verb: VerbConjugation) {
        viewModelScope.launch(dispatchers.io) { speechSynthesizer.speakQuietly(verb.infinitive) }
    }

    private suspend fun createWordWithStatus(verb: VerbConjugation) {
        val id = runSuspendCatching { ensureWord(verb.infinitive, verb.translation) }
            .onFailure { Log.w(TAG, "A verb could not be added to the vocabulary", it) }
            .getOrNull()
        if (id == null) {
            _uiState.update { it.copy(pending = (it.pending - verb.infinitive).toImmutableMap()) }
            return
        }
        val wanted = _uiState.value.pending[verb.infinitive] ?: return
        saveStatus(VocabularyId(id), wanted)
        val found = runSuspendCatching { findWords(listOf(verb.infinitive)) }.getOrDefault(emptyMap())
        _uiState.update {
            it.copy(words = (it.words + found).toImmutableMap(), pending = (it.pending - verb.infinitive).toImmutableMap())
        }
    }

    private suspend fun saveStatus(
        id: VocabularyId,
        status: WordStatus,
    ) {
        runSuspendCatching { setWordStatus(id, status) }.onFailure { Log.w(TAG, "A verb's status could not be saved", it) }
    }

    fun onVerbDeleted(infinitive: String) {
        viewModelScope.launch(dispatchers.io) {
            deleteVerb(infinitive)
            refresh()
        }
    }

    fun onRestoreAll() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch(dispatchers.io) {
            restoreVerbs()
            refresh()
        }
    }

    private suspend fun refresh() {
        val canRestore = hasDeletedVerbs()
        _uiState.update { it.copy(canRestore = canRestore) }
        loadFirstPage()
    }
}
