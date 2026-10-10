package com.lexicon.presentation.course

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.boundary.LessonAudioLibrary
import com.lexicon.boundary.LessonAudioPlayer
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.common.runSuspendCatching
import com.lexicon.interactors.course.GetLessonProgressUseCase
import com.lexicon.interactors.course.GetLessonScriptUseCase
import com.lexicon.interactors.course.GetLessonUseCase
import com.lexicon.interactors.course.LessonResults
import com.lexicon.interactors.course.LessonSession
import com.lexicon.interactors.course.ObserveCoursesUseCase
import com.lexicon.interactors.course.ReviewWritingUseCase
import com.lexicon.interactors.course.SaveLessonProgressUseCase
import com.lexicon.interactors.course.SetLessonCompletedUseCase
import com.lexicon.interactors.course.WritingReviewOutcome
import com.lexicon.model.course.LessonId
import com.lexicon.model.course.LessonProgress
import com.lexicon.model.course.LessonScreen
import com.lexicon.model.course.LessonTrack
import com.lexicon.model.course.allTracks
import com.lexicon.presentation.common.speakQuietly
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "LessonFlow"

enum class ReviewProblem { OFFLINE, UNAVAILABLE }

sealed interface LessonFlowUiState {
    data object Loading : LessonFlowUiState

    data object NotFound : LessonFlowUiState

    data class Loaded(
        val session: LessonSession,
        val lessonNumber: Int?,
        val lessonTitle: String?,
        val tracks: TrackStates = TrackStates(),
        val openTranscripts: Set<String> = emptySet(),
        val isReviewing: Boolean = false,
        val reviewProblem: ReviewProblem? = null,
        val results: LessonResults? = null,
        val nextLessonId: LessonId? = null,
    ) : LessonFlowUiState
}

class LessonFlowViewModel(
    savedStateHandle: SavedStateHandle,
    private val getLessonScript: GetLessonScriptUseCase,
    private val getLessonProgress: GetLessonProgressUseCase,
    private val saveLessonProgress: SaveLessonProgressUseCase,
    private val setLessonCompleted: SetLessonCompletedUseCase,
    private val getLesson: GetLessonUseCase,
    private val observeCourses: ObserveCoursesUseCase,
    private val reviewWriting: ReviewWritingUseCase,
    private val speechSynthesizer: SpeechSynthesizer,
    audioLibrary: LessonAudioLibrary,
    audioPlayer: LessonAudioPlayer,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val lessonId = LessonId(savedStateHandle.get<String>(LESSON_ID_ARG).orEmpty())

    private data class Content(
        val session: LessonSession?,
        val lessonNumber: Int? = null,
        val lessonTitle: String? = null,
        val openTranscripts: Set<String> = emptySet(),
        val isReviewing: Boolean = false,
        val reviewProblem: ReviewProblem? = null,
        val results: LessonResults? = null,
        val nextLessonId: LessonId? = null,
    )

    private val content = MutableStateFlow<Content?>(null)

    private val tracks = LessonTracks(audioLibrary, audioPlayer, dispatchers)

    private val pendingSaves = Channel<LessonProgress>(Channel.CONFLATED)

    val uiState: StateFlow<LessonFlowUiState> =
        combine(content, tracks.states) { loaded, tracks ->
            when {
                loaded == null -> LessonFlowUiState.Loading
                loaded.session == null -> LessonFlowUiState.NotFound
                else ->
                    LessonFlowUiState.Loaded(
                        session = loaded.session,
                        lessonNumber = loaded.lessonNumber,
                        lessonTitle = loaded.lessonTitle,
                        tracks = tracks,
                        openTranscripts = loaded.openTranscripts,
                        isReviewing = loaded.isReviewing,
                        reviewProblem = loaded.reviewProblem,
                        results = loaded.results,
                        nextLessonId = loaded.nextLessonId,
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
            val session = script?.let { LessonSession.resume(it, getLessonProgress(lessonId)) }
            val lesson = getLesson(lessonId)
            content.value = Content(session = session, lessonNumber = lesson?.number, lessonTitle = lesson?.title)
            session?.let(::preload)
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

    fun onLineTapped(key: String) = change { it.withPositionToggled(key) }

    fun onCheck() {
        val session = content.value?.session ?: return
        val screen = session.screen
        if (screen !is LessonScreen.FreeWriting) {
            change { it.check() }
            return
        }
        if (!session.canCheck || content.value?.isReviewing == true) return
        content.update { it?.copy(isReviewing = true, reviewProblem = null) }
        viewModelScope.launch(dispatchers.io) {
            val answers = screen.fields.indices.map { session.answer(screen.id, it.toString()) }
            val outcome = runSuspendCatching { reviewWriting(screen, answers) }
                .onFailure { Log.w(TAG, "The dialogues could not be reviewed", it) }
                .getOrDefault(WritingReviewOutcome.Unavailable)
            content.update { it?.copy(isReviewing = false, reviewProblem = outcome.problem()) }
            change { current -> if (outcome is WritingReviewOutcome.Reviewed) current.withReview(outcome.review) else current.check() }
        }
    }

    fun onNext() {
        val session = content.value?.session ?: return
        if (session.needsCheck) return
        val next = session.next()
        change { next }
        if (session.isLastScreen && next.isAtEnd) complete(next)
    }

    fun onBack() = change { it.previous() }

    fun onTranscriptToggled(file: String) =
        content.update {
            it?.copy(
                openTranscripts = if (file in it.openTranscripts) it.openTranscripts - file else it.openTranscripts + file,
            )
        }

    fun onSpeak(text: String) {
        viewModelScope.launch(dispatchers.io) { speechSynthesizer.speakQuietly(text) }
    }

    fun onPlay(track: LessonTrack) = tracks.toggle(viewModelScope, track.file, track.remoteId)

    fun onSeek(
        track: LessonTrack,
        positionMs: Long,
    ) = tracks.seek(track.file, positionMs)

    private fun complete(session: LessonSession) {
        tracks.stop()
        viewModelScope.launch(dispatchers.io) {
            runSuspendCatching { setLessonCompleted(lessonId, true) }
                .onFailure { Log.w(TAG, "The lesson could not be marked as done", it) }
            val next = runSuspendCatching { nextLessonId() }
                .onFailure { Log.w(TAG, "The next lesson could not be found", it) }
                .getOrNull()
            content.update { it?.copy(results = session.results, nextLessonId = next) }
        }
    }

    private suspend fun nextLessonId(): LessonId? {
        val lessons = observeCourses().first().firstOrNull { course -> course.lessons.any { it.id == lessonId } }?.lessons ?: return null
        val sorted = lessons.sortedBy { it.number }
        return sorted.getOrNull(sorted.indexOfFirst { it.id == lessonId } + 1)?.id
    }

    override fun onCleared() = tracks.stop()

    private fun preload(session: LessonSession) =
        session.screen.allTracks().forEach { tracks.preload(viewModelScope, it.file, it.remoteId) }

    private fun change(transform: (LessonSession) -> LessonSession) {
        var moved = false
        content.update { current ->
            val session = current?.session ?: return@update current
            val next = transform(session)
            moved = next.index != session.index
            current.copy(
                session = next,
                openTranscripts = if (moved) emptySet() else current.openTranscripts,
                reviewProblem = current.reviewProblem.takeUnless { moved },
            )
        }
        if (moved) {
            tracks.stop()
            content.value?.session?.let(::preload)
        }
        content.value?.session?.let { pendingSaves.trySend(it.progress) }
    }

    private fun WritingReviewOutcome.problem(): ReviewProblem? =
        when (this) {
            is WritingReviewOutcome.Reviewed -> null
            WritingReviewOutcome.Offline -> ReviewProblem.OFFLINE
            WritingReviewOutcome.Unavailable -> ReviewProblem.UNAVAILABLE
        }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
