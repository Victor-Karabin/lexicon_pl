package com.lexicon.presentation.course

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.common.runSuspendCatching
import com.lexicon.interactors.course.GetLastLessonResultsUseCase
import com.lexicon.interactors.course.GetLessonProgressUseCase
import com.lexicon.interactors.course.GetLessonScriptUseCase
import com.lexicon.interactors.course.GetLessonUseCase
import com.lexicon.interactors.course.GetLessonVocabularyUseCase
import com.lexicon.interactors.course.Lesson
import com.lexicon.interactors.course.LessonSession
import com.lexicon.interactors.presets.FindWordsUseCase
import com.lexicon.interactors.presets.ObserveWordStatusesUseCase
import com.lexicon.interactors.presets.SetWordStatusUseCase
import com.lexicon.model.course.LessonId
import com.lexicon.model.course.LessonPhrase
import com.lexicon.model.course.LessonScript
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import com.lexicon.model.vocabulary.WordStatus
import com.lexicon.model.vocabulary.statusOf
import com.lexicon.presentation.common.speakQuietly
import com.lexicon.presentation.presets.sortedForDisplay
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "Lesson"
private const val SHORTEST_PLAIN_WORD = 3

sealed interface LessonWord {
    val key: Any
    val text: String

    data class Entry(val word: Word) : LessonWord {
        override val key: Any get() = word.id.value
        override val text: String get() = word.text
    }

    data class Plain(val phrase: LessonPhrase) : LessonWord {
        override val key: Any get() = "plain:${phrase.polish}"
        override val text: String get() = phrase.polish
    }
}

sealed interface LessonUiState {
    data object Loading : LessonUiState

    data object NotFound : LessonUiState

    data class Loaded(
        val lesson: Lesson,
        val words: ImmutableList<LessonWord> = persistentListOf(),
        val wordStatuses: Map<VocabularyId, WordStatus> = emptyMap(),
        val isLoadingWords: Boolean = true,
        val hasScript: Boolean = false,
        val isScriptStarted: Boolean = false,
        val hasResults: Boolean = false,
    ) : LessonUiState
}

const val LESSON_ID_ARG = "lessonId"

class LessonViewModel(
    savedStateHandle: SavedStateHandle,
    private val getLesson: GetLessonUseCase,
    private val getLessonVocabulary: GetLessonVocabularyUseCase,
    private val getLessonScript: GetLessonScriptUseCase,
    private val getLessonProgress: GetLessonProgressUseCase,
    private val getLastLessonResults: GetLastLessonResultsUseCase,
    private val findWords: FindWordsUseCase,
    private val setWordStatus: SetWordStatusUseCase,
    observeWordStatuses: ObserveWordStatusesUseCase,
    private val dispatchers: DispatcherProvider,
    private val speechSynthesizer: SpeechSynthesizer,
) : ViewModel() {
    private val lessonId = LessonId(savedStateHandle.get<String>(LESSON_ID_ARG).orEmpty())

    private data class Content(
        val lesson: Lesson?,
        val words: ImmutableList<LessonWord> = persistentListOf(),
        val wordsLoaded: Boolean = false,
        val hasScript: Boolean = false,
        val isScriptStarted: Boolean = false,
        val hasResults: Boolean = false,
    )

    private val content = MutableStateFlow<Content?>(null)

    val uiState: StateFlow<LessonUiState> =
        combine(content, observeWordStatuses()) { loaded, statuses ->
            when {
                loaded == null -> LessonUiState.Loading
                loaded.lesson == null -> LessonUiState.NotFound
                else ->
                    LessonUiState.Loaded(
                        lesson = loaded.lesson,
                        words = loaded.words,
                        wordStatuses = statuses,
                        isLoadingWords = !loaded.wordsLoaded,
                        hasScript = loaded.hasScript,
                        isScriptStarted = loaded.isScriptStarted,
                        hasResults = loaded.hasResults,
                    )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = LessonUiState.Loading,
        )

    init {
        viewModelScope.launch(dispatchers.io) { load() }
    }

    fun onResumed() {
        if (content.value?.lesson == null) return
        viewModelScope.launch(dispatchers.io) { load() }
    }

    fun onPronounce(text: String) {
        viewModelScope.launch(dispatchers.io) {
            speechSynthesizer.speakQuietly(text)
        }
    }

    fun onWordStatusCycled(id: VocabularyId) {
        val loaded = uiState.value as? LessonUiState.Loaded ?: return
        val current = loaded.wordStatuses.statusOf(id)

        viewModelScope.launch(dispatchers.io) { setWordStatus(id, current.next()) }
    }

    private suspend fun load() {
        val lesson = getLesson(lessonId)
        if (lesson == null) {
            content.value = Content(lesson = null)
            return
        }
        val script = getLessonScript(lessonId)
        val hasScript = script != null
        val isScriptStarted = script != null && LessonSession.isInProgress(script, getLessonProgress(lessonId))
        val hasResults = script != null && getLastLessonResults(lessonId) != null
        content.value = Content(lesson = lesson, hasScript = hasScript, isScriptStarted = isScriptStarted, hasResults = hasResults)
        content.value = Content(
            lesson = lesson,
            words = (getLessonVocabulary(lessonId).map(LessonWord::Entry) + plainNewWords(script).map(LessonWord::Plain))
                .sortedForDisplay { it.text }
                .toImmutableList(),
            wordsLoaded = true,
            hasScript = hasScript,
            isScriptStarted = isScriptStarted,
            hasResults = hasResults,
        )
    }

    private suspend fun plainNewWords(script: LessonScript?): List<LessonPhrase> {
        val phrases = script?.newWords?.values?.flatMap { it.words }?.distinctBy { it.polish }.orEmpty()
        if (phrases.isEmpty()) return emptyList()
        val found = runSuspendCatching { findWords(phrases.map { it.polish }) }
            .onFailure { Log.w(TAG, "New words could not be matched to the vocabulary", it) }
            .getOrNull() ?: return emptyList()
        return phrases.filter { it.polish !in found && it.polish.trimEnd('…', '?', '!', '.').length >= SHORTEST_PLAIN_WORD }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
