package com.lexicon.presentation.course

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.course.GetLessonUseCase
import com.lexicon.interactors.course.GetLessonVocabularyUseCase
import com.lexicon.interactors.course.Lesson
import com.lexicon.interactors.course.LessonId
import com.lexicon.interactors.course.SetLessonCompletedUseCase
import com.lexicon.interactors.presets.ObserveStudySetIdsUseCase
import com.lexicon.interactors.presets.ToggleWordInStudySetUseCase
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
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

sealed interface LessonUiState {
    data object Loading : LessonUiState

    data object NotFound : LessonUiState

    data class Loaded(
        val lesson: Lesson,
        val words: ImmutableList<Word> = persistentListOf(),
        val isLoadingWords: Boolean = true,
    ) : LessonUiState
}

const val LESSON_ID_ARG = "lessonId"

class LessonViewModel(
    savedStateHandle: SavedStateHandle,
    private val getLesson: GetLessonUseCase,
    private val getLessonVocabulary: GetLessonVocabularyUseCase,
    private val setLessonCompleted: SetLessonCompletedUseCase,
    private val toggleWordInStudySet: ToggleWordInStudySetUseCase,
    observeStudySetIds: ObserveStudySetIdsUseCase,
    private val dispatchers: DispatcherProvider,
    private val speechSynthesizer: SpeechSynthesizer,
) : ViewModel() {
    private val lessonId = LessonId(savedStateHandle.get<String>(LESSON_ID_ARG).orEmpty())

    private data class Content(
        val lesson: Lesson?,
        val words: List<Word> = emptyList(),
        val wordsLoaded: Boolean = false,
    )

    private val content = MutableStateFlow<Content?>(null)

    val uiState: StateFlow<LessonUiState> =
        combine(content, observeStudySetIds()) { loaded, studySet ->
            when {
                loaded == null -> LessonUiState.Loading
                loaded.lesson == null -> LessonUiState.NotFound
                else ->
                    LessonUiState.Loaded(
                        lesson = loaded.lesson,
                        words = loaded.words
                            .map { it.copy(isInStudySet = it.id in studySet) }
                            .toImmutableList(),
                        isLoadingWords = !loaded.wordsLoaded,
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

    fun onCompletedToggled(isCompleted: Boolean) {
        viewModelScope.launch(dispatchers.io) {
            setLessonCompleted(lessonId, isCompleted)
            content.update { it?.copy(lesson = it.lesson?.copy(isCompleted = isCompleted)) }
        }
    }

    fun onPronounceWord(word: Word) {
        viewModelScope.launch(dispatchers.io) {
            runCatching { speechSynthesizer.speak(word.text) }
        }
    }

    fun onWordStudySetToggled(
        id: VocabularyId,
        isInStudySet: Boolean,
    ) {
        viewModelScope.launch(dispatchers.io) { toggleWordInStudySet(id, isInStudySet) }
    }

    private suspend fun load() {
        val lesson = getLesson(lessonId)
        if (lesson == null) {
            content.value = Content(lesson = null)
            return
        }
        content.value = Content(lesson = lesson)
        content.value = Content(
            lesson = lesson,
            words = getLessonVocabulary(lessonId),
            wordsLoaded = true,
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
