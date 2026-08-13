package com.lexicon.presentation.course

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.android.LessonAudioLibrary
import com.lexicon.android.LessonAudioPlayer
import com.lexicon.android.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.course.GetLessonUseCase
import com.lexicon.interactors.course.GetLessonVocabularyUseCase
import com.lexicon.interactors.course.Lesson
import com.lexicon.interactors.course.LessonAudio
import com.lexicon.interactors.course.LessonId
import com.lexicon.interactors.course.SetLessonCompletedUseCase
import com.lexicon.interactors.presets.ObserveFavouriteWordIdsUseCase
import com.lexicon.interactors.presets.PresetWord
import com.lexicon.interactors.presets.ToggleWordFavouriteUseCase
import com.lexicon.interactors.presets.VocabularyId
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
        val words: ImmutableList<PresetWord> = persistentListOf(),
        val isLoadingWords: Boolean = true,
        /** Tracks already on the device; the rest are fetched on first play. */
        val availableAudio: Set<String> = emptySet(),
        /** The track currently being fetched, so its chip can show progress. */
        val downloadingAudio: String? = null,
        /** The track currently playing, so its chip offers pause instead of play. */
        val playingAudio: String? = null,
    ) : LessonUiState
}

/** A track is playable if it is here already or can be fetched. */
val LessonAudio.isPlayable: Boolean
    get() = remoteId != null

fun LessonUiState.Loaded.isPlayable(track: LessonAudio): Boolean = track.file in availableAudio || track.isPlayable

val LessonUiState.Loaded.hasPlayableAudio: Boolean
    get() = lesson.audio.any { isPlayable(it) }

const val LESSON_ID_ARG = "lessonId"

class LessonViewModel(
    savedStateHandle: SavedStateHandle,
    private val getLesson: GetLessonUseCase,
    private val getLessonVocabulary: GetLessonVocabularyUseCase,
    private val setLessonCompleted: SetLessonCompletedUseCase,
    private val toggleWordFavourite: ToggleWordFavouriteUseCase,
    private val audioLibrary: LessonAudioLibrary,
    private val audioPlayer: LessonAudioPlayer,
    observeFavouriteWordIds: ObserveFavouriteWordIdsUseCase,
    private val dispatchers: DispatcherProvider,
    private val speechSynthesizer: SpeechSynthesizer,
) : ViewModel() {
    private val lessonId = LessonId(savedStateHandle.get<String>(LESSON_ID_ARG).orEmpty())

    private data class Content(
        val lesson: Lesson?,
        val words: List<PresetWord> = emptyList(),
        val wordsLoaded: Boolean = false,
        val availableAudio: Set<String> = emptySet(),
        val downloadingAudio: String? = null,
    )

    private val content = MutableStateFlow<Content?>(null)

    val uiState: StateFlow<LessonUiState> =
        combine(content, observeFavouriteWordIds(), audioPlayer.playingFile) { loaded, favourites, playing ->
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
                        downloadingAudio = loaded.downloadingAudio,
                        playingAudio = playing,
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

    /** Tapping the track that is playing pauses it; anything else starts from the top. */
    fun onPlayAudio(track: LessonAudio) {
        if (audioPlayer.playingFile.value == track.file) {
            audioPlayer.pause()
            return
        }

        viewModelScope.launch(dispatchers.io) {
            val cached = audioLibrary.localPathOrNull(track.file)
            if (cached == null) content.update { it?.copy(downloadingAudio = track.file) }

            val path = cached ?: audioLibrary.pathOrNull(track.file, track.remoteId)
            content.update {
                it?.copy(
                    downloadingAudio = null,
                    availableAudio = if (path == null) it.availableAudio else it.availableAudio + track.file,
                )
            }
            if (path != null) runCatching { audioPlayer.play(track.file, path) }
        }
    }

    override fun onCleared() {
        // The player outlives this screen, so leaving the lesson has to silence it.
        audioPlayer.stop()
    }

    /** Reads the Polish out loud; the translation is not what a learner needs to hear. */
    fun onPronounceWord(word: PresetWord) {
        viewModelScope.launch(dispatchers.io) {
            runCatching { speechSynthesizer.speak(word.text) }
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
