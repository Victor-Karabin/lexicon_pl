package com.lexicon.presentation.course

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.android.AudioPlayer
import com.lexicon.android.LessonAudioLibrary
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.course.GetLessonUseCase
import com.lexicon.interactors.course.GetLessonVocabularyUseCase
import com.lexicon.interactors.course.Lesson
import com.lexicon.interactors.course.LessonId
import com.lexicon.interactors.course.SetLessonCompletedUseCase
import com.lexicon.interactors.presets.ObserveFavouriteWordIdsUseCase
import com.lexicon.interactors.presets.PresetWord
import com.lexicon.interactors.presets.ToggleWordFavouriteUseCase
import com.lexicon.interactors.presets.VocabularyId
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
import javax.inject.Inject

sealed interface LessonUiState {
    data object Loading : LessonUiState

    data object NotFound : LessonUiState

    data class Loaded(
        val lesson: Lesson,
        val words: ImmutableList<PresetWord> = persistentListOf(),
        val isLoadingWords: Boolean = true,
        /** Tracks actually present on the device; the rest render as disabled. */
        val availableAudio: Set<String> = emptySet(),
    ) : LessonUiState
}

val LessonUiState.Loaded.hasAudio: Boolean
    get() = lesson.audio.any { it.file in availableAudio }

const val LESSON_ID_ARG = "lessonId"

@HiltViewModel
class LessonViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val getLesson: GetLessonUseCase,
        private val getLessonVocabulary: GetLessonVocabularyUseCase,
        private val setLessonCompleted: SetLessonCompletedUseCase,
        private val toggleWordFavourite: ToggleWordFavouriteUseCase,
        private val audioLibrary: LessonAudioLibrary,
        private val audioPlayer: AudioPlayer,
        observeFavouriteWordIds: ObserveFavouriteWordIdsUseCase,
        private val dispatchers: DispatcherProvider,
    ) : ViewModel() {
        private val lessonId = LessonId(savedStateHandle.get<String>(LESSON_ID_ARG).orEmpty())

        private data class Content(
            val lesson: Lesson?,
            val words: List<PresetWord> = emptyList(),
            val wordsLoaded: Boolean = false,
            val availableAudio: Set<String> = emptySet(),
        )

        private val content = MutableStateFlow<Content?>(null)

        val uiState: StateFlow<LessonUiState> =
            combine(content, observeFavouriteWordIds()) { loaded, favourites ->
                when {
                    loaded == null -> LessonUiState.Loading
                    loaded.lesson == null -> LessonUiState.NotFound
                    else ->
                        LessonUiState.Loaded(
                            lesson = loaded.lesson,
                            words = loaded.words
                                .map { it.copy(isFavourite = it.id in favourites) }
                                .toImmutableList(),
                            isLoadingWords = !loaded.wordsLoaded,
                            availableAudio = loaded.availableAudio,
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

        fun onPlayAudio(file: String) {
            viewModelScope.launch(dispatchers.io) {
                val path = audioLibrary.pathOrNull(file) ?: return@launch
                runCatching { audioPlayer.play(path) }
            }
        }

        fun onWordFavouriteToggled(
            id: VocabularyId,
            isFavourite: Boolean,
        ) {
            viewModelScope.launch(dispatchers.io) { toggleWordFavourite(id, isFavourite) }
        }

        private suspend fun load() {
            val lesson = getLesson(lessonId)
            if (lesson == null) {
                content.value = Content(lesson = null)
                return
            }
            val available = audioLibrary.availableFiles()
            content.value = Content(lesson = lesson, availableAudio = available)
            content.value = Content(
                lesson = lesson,
                words = getLessonVocabulary(lessonId),
                wordsLoaded = true,
                availableAudio = available,
            )
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
