package com.lexicon.presentation.course

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.course.GetLessonProgressUseCase
import com.lexicon.interactors.course.GetLessonScriptUseCase
import com.lexicon.interactors.course.GetLessonUseCase
import com.lexicon.interactors.course.GetLessonVocabularyUseCase
import com.lexicon.interactors.course.Lesson
import com.lexicon.interactors.course.LessonSession
import com.lexicon.interactors.presets.ObserveWordStatusesUseCase
import com.lexicon.interactors.presets.SetWordStatusUseCase
import com.lexicon.model.course.LessonId
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import com.lexicon.model.vocabulary.WordStatus
import com.lexicon.model.vocabulary.statusOf
import com.lexicon.presentation.common.speakQuietly
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface LessonUiState {
    data object Loading : LessonUiState

    data object NotFound : LessonUiState

    data class Loaded(
        val lesson: Lesson,
        val words: ImmutableList<Word> = persistentListOf(),
        val wordStatuses: Map<VocabularyId, WordStatus> = emptyMap(),
        val isLoadingWords: Boolean = true,
        val hasScript: Boolean = false,
        val isScriptStarted: Boolean = false,
    ) : LessonUiState
}

const val LESSON_ID_ARG = "lessonId"

class LessonViewModel(
    savedStateHandle: SavedStateHandle,
    private val getLesson: GetLessonUseCase,
    private val getLessonVocabulary: GetLessonVocabularyUseCase,
    private val getLessonScript: GetLessonScriptUseCase,
    private val getLessonProgress: GetLessonProgressUseCase,
    private val setWordStatus: SetWordStatusUseCase,
    observeWordStatuses: ObserveWordStatusesUseCase,
    private val dispatchers: DispatcherProvider,
    private val speechSynthesizer: SpeechSynthesizer,
) : ViewModel() {
    private val lessonId = LessonId(savedStateHandle.get<String>(LESSON_ID_ARG).orEmpty())

    private data class Content(
        val lesson: Lesson?,
        val words: List<Word> = emptyList(),
        val wordsLoaded: Boolean = false,
        val hasScript: Boolean = false,
        val isScriptStarted: Boolean = false,
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
                        words = loaded.words.toImmutableList(),
                        wordStatuses = statuses,
                        isLoadingWords = !loaded.wordsLoaded,
                        hasScript = loaded.hasScript,
                        isScriptStarted = loaded.isScriptStarted,
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

    fun onPronounceWord(word: Word) {
        viewModelScope.launch(dispatchers.io) {
            speechSynthesizer.speakQuietly(word.text)
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
        content.value = Content(lesson = lesson, hasScript = hasScript, isScriptStarted = isScriptStarted)
        content.value = Content(
            lesson = lesson,
            words = getLessonVocabulary(lessonId),
            wordsLoaded = true,
            hasScript = hasScript,
            isScriptStarted = isScriptStarted,
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
