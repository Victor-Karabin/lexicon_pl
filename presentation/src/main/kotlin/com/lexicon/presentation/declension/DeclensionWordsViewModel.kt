package com.lexicon.presentation.declension

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.common.runSuspendCatching
import com.lexicon.interactors.declension.LoadDeclinableWordsUseCase
import com.lexicon.interactors.presets.ObserveWordStatusesUseCase
import com.lexicon.interactors.presets.SetWordStatusUseCase
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import com.lexicon.model.vocabulary.WordStatus
import com.lexicon.model.vocabulary.statusOf
import com.lexicon.presentation.common.speakQuietly
import com.lexicon.presentation.conjugation.SEARCH_SETTLE_MS
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

private const val TAG = "DeclensionWords"

data class DeclensionWordsUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val words: ImmutableList<Word> = persistentListOf(),
    val statuses: ImmutableMap<VocabularyId, WordStatus> = persistentMapOf(),
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val nextOffset: Int = 0,
) {
    fun statusOf(word: Word): WordStatus = statuses.statusOf(word.id)
}

class DeclensionWordsViewModel(
    private val loadWords: LoadDeclinableWordsUseCase,
    private val setWordStatus: SetWordStatusUseCase,
    observeWordStatuses: ObserveWordStatusesUseCase,
    private val speechSynthesizer: SpeechSynthesizer,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DeclensionWordsUiState())
    val uiState: StateFlow<DeclensionWordsUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var pageJob: Job? = null

    init {
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

    fun onMoreWords() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return

        _uiState.update { it.copy(isLoadingMore = true) }
        pageJob = viewModelScope.launch(dispatchers.io) {
            val more = loadWords.page(query = state.query, offset = state.nextOffset)
            _uiState.update {
                it.copy(
                    words = (it.words + more.words).distinctBy { word -> word.id }.toImmutableList(),
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
            val first = loadWords.page(query = _uiState.value.query, offset = 0)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    words = first.words,
                    nextOffset = first.nextOffset,
                    hasMore = !first.isLast,
                )
            }
        }
    }

    fun onStatusCycled(word: Word) {
        val next = _uiState.value.statusOf(word).next()
        viewModelScope.launch(dispatchers.io + NonCancellable) {
            runSuspendCatching { setWordStatus(word.id, next) }.onFailure { Log.w(TAG, "A word's status could not be saved", it) }
        }
    }

    fun onPronounce(word: Word) {
        viewModelScope.launch(dispatchers.io) { speechSynthesizer.speakQuietly(word.text) }
    }
}
