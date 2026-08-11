package com.lexicon.presentation.course

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.android.LessonAudioLibrary
import com.lexicon.android.LessonAudioPlayer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.course.CheckExerciseAnswerUseCase
import com.lexicon.interactors.course.GetLessonUseCase
import com.lexicon.interactors.course.LessonExercise
import com.lexicon.interactors.course.LessonId
import com.lexicon.presentation.common.AnswerState
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

sealed interface ExerciseUiState {
    data object Loading : ExerciseUiState

    data object NotFound : ExerciseUiState

    data class Loaded(
        val exercise: LessonExercise,
        /** One entry per question: the chosen option, or the text typed per blank. */
        val responses: ImmutableList<ImmutableList<String>> = persistentListOf(),
        val answerState: AnswerState = AnswerState.Unanswered,
        val correctCount: Int = 0,
        val isPlaying: Boolean = false,
        val isAudioMissing: Boolean = false,
    ) : ExerciseUiState
}

const val EXERCISE_ID_ARG = "exerciseId"

@HiltViewModel
class ExerciseViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val getLesson: GetLessonUseCase,
        private val checkAnswer: CheckExerciseAnswerUseCase,
        private val audioLibrary: LessonAudioLibrary,
        private val audioPlayer: LessonAudioPlayer,
        private val dispatchers: DispatcherProvider,
    ) : ViewModel() {
        private val lessonId = LessonId(savedStateHandle.get<String>(LESSON_ID_ARG).orEmpty())
        private val exerciseId = savedStateHandle.get<String>(EXERCISE_ID_ARG).orEmpty()

        private data class Content(
            val exercise: LessonExercise?,
            /** The lesson track this exercise plays, which carries the Drive id. */
            val remoteId: String? = null,
            val responses: List<List<String>> = emptyList(),
            val answerState: AnswerState = AnswerState.Unanswered,
            val correctCount: Int = 0,
            val isAudioMissing: Boolean = false,
        )

        private val content = MutableStateFlow<Content?>(null)

        val uiState: StateFlow<ExerciseUiState> =
            combine(content, audioPlayer.playingFile) { loaded, playing ->
                when {
                    loaded == null -> ExerciseUiState.Loading
                    loaded.exercise == null -> ExerciseUiState.NotFound
                    else ->
                        ExerciseUiState.Loaded(
                            exercise = loaded.exercise,
                            responses = loaded.responses.map { it.toImmutableList() }.toImmutableList(),
                            answerState = loaded.answerState,
                            correctCount = loaded.correctCount,
                            isPlaying = playing != null && playing == loaded.exercise.audioFile,
                            isAudioMissing = loaded.isAudioMissing,
                        )
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = ExerciseUiState.Loading,
            )

        init {
            viewModelScope.launch(dispatchers.io) {
                val lesson = getLesson(lessonId)
                val exercise = lesson?.exercises?.firstOrNull { it.id == exerciseId }
                content.value = Content(
                    exercise = exercise,
                    remoteId = lesson?.audio?.firstOrNull { it.file == exercise?.audioFile }?.remoteId,
                    responses = blankResponses(exercise),
                )
            }
        }

        fun onPlayAudio() {
            val exercise = content.value?.exercise ?: return
            val file = exercise.audioFile ?: return
            if (audioPlayer.playingFile.value == file) {
                audioPlayer.pause()
                return
            }
            viewModelScope.launch(dispatchers.io) {
                val path = audioLibrary.pathOrNull(file, content.value?.remoteId)
                if (path == null) {
                    content.update { it?.copy(isAudioMissing = true) }
                    return@launch
                }
                runCatching { audioPlayer.play(file, path) }
            }
        }

        fun onOptionSelected(
            index: Int,
            option: String,
        ) = updateResponse(index, 0, option)

        fun onGapChanged(
            index: Int,
            gap: Int,
            value: String,
        ) = updateResponse(index, gap, value)

        /** Marks every question at once: the exercise is a page of the book, not a step. */
        fun onCheck() {
            val current = content.value ?: return
            val exercise = current.exercise ?: return

            val correct = expectedAnswers(exercise).withIndex().sumOf { (index, expected) ->
                expected.withIndex().count { (gap, answer) ->
                    checkAnswer(answer, current.responses.getOrNull(index)?.getOrNull(gap).orEmpty())
                }
            }
            val total = expectedAnswers(exercise).sumOf { it.size }
            content.update {
                it?.copy(
                    correctCount = correct,
                    answerState = if (correct == total) AnswerState.Correct else AnswerState.Incorrect(),
                )
            }
        }

        fun onRetry() {
            content.update {
                it?.copy(
                    responses = blankResponses(it.exercise),
                    answerState = AnswerState.Unanswered,
                    correctCount = 0,
                )
            }
        }

        override fun onCleared() = audioPlayer.stop()

        private fun updateResponse(
            index: Int,
            gap: Int,
            value: String,
        ) {
            content.update { current ->
                current ?: return@update null
                val responses = current.responses.toMutableList()
                val row = responses.getOrNull(index)?.toMutableList() ?: return@update current
                while (row.size <= gap) row.add("")
                row[gap] = value
                responses[index] = row
                current.copy(responses = responses)
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }

/** The answers each question expects, one list per question. */
internal fun expectedAnswers(exercise: LessonExercise): List<List<String>> =
    when (exercise) {
        is LessonExercise.Repeat -> emptyList()
        is LessonExercise.MinimalPair -> exercise.items.map { listOf(it.answer) }
        is LessonExercise.GapFill -> exercise.items.map { it.answers }
    }

private fun blankResponses(exercise: LessonExercise?): List<List<String>> =
    exercise?.let { expectedAnswers(it).map { answers -> List(answers.size) { "" } } }.orEmpty()
