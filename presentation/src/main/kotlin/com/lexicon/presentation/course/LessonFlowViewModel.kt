package com.lexicon.presentation.course

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.boundary.LessonAudioLibrary
import com.lexicon.boundary.LessonAudioPlayer
import com.lexicon.common.DispatcherProvider
import com.lexicon.common.runSuspendCatching
import com.lexicon.interactors.course.GetLessonProgressUseCase
import com.lexicon.interactors.course.GetLessonScriptUseCase
import com.lexicon.interactors.course.LessonSession
import com.lexicon.interactors.course.SaveLessonProgressUseCase
import com.lexicon.interactors.course.SetLessonCompletedUseCase
import com.lexicon.model.course.LessonId
import com.lexicon.model.course.LessonProgress
import com.lexicon.model.course.LessonTrack
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "LessonFlow"

sealed interface LessonFlowUiState {
    data object Loading : LessonFlowUiState

    data object NotFound : LessonFlowUiState

    data class Loaded(
        val session: LessonSession,
        val playingFile: String? = null,
        val missingAudio: Set<String> = emptySet(),
        val transcriptOpen: Boolean = false,
        val isCompleted: Boolean = false,
    ) : LessonFlowUiState
}

class LessonFlowViewModel(
    savedStateHandle: SavedStateHandle,
    private val getLessonScript: GetLessonScriptUseCase,
    private val getLessonProgress: GetLessonProgressUseCase,
    private val saveLessonProgress: SaveLessonProgressUseCase,
    private val setLessonCompleted: SetLessonCompletedUseCase,
    private val audioLibrary: LessonAudioLibrary,
    private val audioPlayer: LessonAudioPlayer,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val lessonId = LessonId(savedStateHandle.get<String>(LESSON_ID_ARG).orEmpty())

    private data class Content(
        val session: LessonSession?,
        val missingAudio: Set<String> = emptySet(),
        val transcriptOpen: Boolean = false,
        val isCompleted: Boolean = false,
    )

    private val content = MutableStateFlow<Content?>(null)

    private val pendingSaves = Channel<LessonProgress>(Channel.CONFLATED)

    val uiState: StateFlow<LessonFlowUiState> =
        combine(content, audioPlayer.playingFile) { loaded, playing ->
            when {
                loaded == null -> LessonFlowUiState.Loading
                loaded.session == null -> LessonFlowUiState.NotFound
                else ->
                    LessonFlowUiState.Loaded(
                        session = loaded.session,
                        playingFile = playing,
                        missingAudio = loaded.missingAudio,
                        transcriptOpen = loaded.transcriptOpen,
                        isCompleted = loaded.isCompleted,
                    )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = LessonFlowUiState.Loading,
        )

    init {
        viewModelScope.launch(dispatchers.io) {
            val script = getLessonScript(lessonId)
            content.value = Content(session = script?.let { LessonSession.start(it, getLessonProgress(lessonId)) })
        }
        viewModelScope.launch(dispatchers.io) {
            pendingSaves.consumeAsFlow().collect { progress ->
                runSuspendCatching { saveLessonProgress(lessonId, progress) }
                    .onFailure { Log.w(TAG, "Lesson progress could not be saved", it) }
            }
        }
    }

    fun onAnswerChanged(
        key: String,
        value: String,
    ) = change { it.withAnswer(key, value) }

    fun onCheck() = change { it.check() }

    fun onDone() = change { it.finish().next() }

    fun onShowModel() = change { it.finish() }

    fun onNext() = change { it.next() }

    fun onBack() = change { it.previous() }

    fun onRetryStep() = change { session -> session.stepToRetry?.let(session::retryStep) ?: session }

    fun onTranscriptToggled() = content.update { it?.copy(transcriptOpen = !it.transcriptOpen) }

    fun onFinishLesson() {
        val session = content.value?.session ?: return
        if (!session.isLessonComplete) return
        audioPlayer.stop()
        viewModelScope.launch(dispatchers.io) {
            setLessonCompleted(lessonId, true)
            content.update { it?.copy(isCompleted = true) }
        }
    }

    fun onPlay(track: LessonTrack) {
        if (audioPlayer.playingFile.value == track.file) {
            audioPlayer.pause()
            return
        }
        viewModelScope.launch(dispatchers.io) {
            val path = audioLibrary.pathOrNull(track.file, track.remoteId)
            if (path == null) {
                content.update { it?.copy(missingAudio = it.missingAudio + track.file) }
                return@launch
            }
            runSuspendCatching { audioPlayer.play(track.file, path) }
                .onFailure { Log.w(TAG, "A lesson recording could not be played", it) }
        }
    }

    override fun onCleared() = audioPlayer.stop()

    private fun change(transform: (LessonSession) -> LessonSession) {
        var moved = false
        content.update { current ->
            val session = current?.session ?: return@update current
            val next = transform(session)
            moved = next.index != session.index
            current.copy(
                session = next,
                transcriptOpen = current.transcriptOpen && !moved,
            )
        }
        if (moved) audioPlayer.stop()
        content.value?.session?.let { pendingSaves.trySend(it.progress) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
