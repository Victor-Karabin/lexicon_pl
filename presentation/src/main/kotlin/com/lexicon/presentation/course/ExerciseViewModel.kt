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
import kotlin.random.Random

sealed interface ExerciseUiState {
    data object Loading : ExerciseUiState

    data object NotFound : ExerciseUiState

    data class Loaded(
        val exercise: LessonExercise,
        /** One entry per question: the chosen option, or the text typed per blank. */
        val responses: ImmutableList<ImmutableList<String>> = persistentListOf(),
        /**
         * Whether each of those answers was right, laid out the same way and empty
         * until there has been a marking. Per answer rather than per question so a
         * line with three blanks can show which one of them was wrong.
         */
        val correctness: ImmutableList<ImmutableList<Boolean>> = persistentListOf(),
        val answerState: AnswerState = AnswerState.Unanswered,
        val correctCount: Int = 0,
        val isPlaying: Boolean = false,
        val isAudioMissing: Boolean = false,
        /** The left-hand item waiting for something to pair with, on a matching exercise. */
        val selectedPrompt: Int? = null,
    ) : ExerciseUiState {
        /**
         * The right-hand column of a matching exercise: every answer there is, in an
         * order that is not the prompts' order — otherwise the pairing is given away
         * by the layout. Shuffled from the exercise's own id, so it is the same
         * column every time this exercise is opened.
         */
        val choices: ImmutableList<String>
            get() = (exercise as? LessonExercise.Match)
                ?.items
                ?.map { it.answer }
                ?.shuffled(Random(exercise.id.hashCode()))
                ?.toImmutableList()
                ?: persistentListOf()
    }
}

const val EXERCISE_ID_ARG = "exerciseId"

class ExerciseViewModel(
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
        val correctness: List<List<Boolean>> = emptyList(),
        val answerState: AnswerState = AnswerState.Unanswered,
        val correctCount: Int = 0,
        val isAudioMissing: Boolean = false,
        val selectedPrompt: Int? = null,
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
                        correctness = loaded.correctness.map { it.toImmutableList() }.toImmutableList(),
                        answerState = loaded.answerState,
                        correctCount = loaded.correctCount,
                        isPlaying = playing != null && playing == loaded.exercise.audioFile,
                        isAudioMissing = loaded.isAudioMissing,
                        selectedPrompt = loaded.selectedPrompt,
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

    /**
     * Pairs the chosen left-hand item with a right-hand one.
     *
     * A right-hand answer belongs to one prompt at a time, so giving it to another
     * takes it away from the first rather than leaving it in two places.
     */
    fun onMatchChoiceSelected(choice: String) {
        val current = content.value ?: return
        val prompt = current.selectedPrompt ?: return
        val responses = current.responses.mapIndexed { index, row ->
            when {
                index == prompt -> listOf(choice)
                row.firstOrNull() == choice -> listOf("")
                else -> row
            }
        }
        content.update { it?.copy(responses = responses, selectedPrompt = null) }
    }

    fun onMatchPromptSelected(index: Int) {
        content.update { it?.copy(selectedPrompt = if (it.selectedPrompt == index) null else index) }
    }

    /** Marks every question at once: the exercise is a page of the book, not a step. */
    fun onCheck() {
        val current = content.value ?: return
        val exercise = current.exercise ?: return

        val correctness = expectedAnswers(exercise).mapIndexed { index, expected ->
            expected.mapIndexed { gap, answer ->
                checkAnswer(answer, current.responses.getOrNull(index)?.getOrNull(gap).orEmpty())
            }
        }
        val correct = correctness.sumOf { row -> row.count { it } }
        val total = correctness.sumOf { it.size }
        content.update {
            it?.copy(
                correctness = correctness,
                correctCount = correct,
                answerState = if (correct == total) AnswerState.Correct else AnswerState.Incorrect(),
                selectedPrompt = null,
            )
        }
    }

    fun onRetry() {
        content.update {
            it?.copy(
                responses = blankResponses(it.exercise),
                correctness = emptyList(),
                answerState = AnswerState.Unanswered,
                correctCount = 0,
                selectedPrompt = null,
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
        is LessonExercise.Transcribe -> exercise.items.map { listOf(it.answer) }
        is LessonExercise.Match -> exercise.items.map { listOf(it.answer) }
        is LessonExercise.LetterFill -> exercise.items.map { it.letters }
    }

private fun blankResponses(exercise: LessonExercise?): List<List<String>> =
    exercise?.let { expectedAnswers(it).map { answers -> List(answers.size) { "" } } }.orEmpty()
