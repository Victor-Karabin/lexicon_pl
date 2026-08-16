package com.lexicon.interactors.course

import com.lexicon.interactors.presets.LocalizedText
import com.lexicon.interactors.presets.VocabularyId
import kotlinx.collections.immutable.ImmutableList

data class LessonId(val value: String)

data class CourseId(val value: String)

data class Course(
    val id: CourseId,
    val order: Int,
    val level: String,
    val title: LocalizedText,
    val lessons: ImmutableList<LessonSummary>,
)

data class LessonSummary(
    val id: LessonId,
    val number: Int,
    val title: String,
    val wordCount: Int,
    val isCompleted: Boolean,
    val isUnlocked: Boolean,
)

data class Lesson(
    val id: LessonId,
    val courseId: CourseId,
    val number: Int,
    val title: String,
    val vocabularyIds: ImmutableList<VocabularyId>,
    val audio: ImmutableList<LessonAudio>,
    val exercises: ImmutableList<LessonExercise>,
    val isCompleted: Boolean,
)

data class LessonAudio(
    val file: String,
    val section: String?,
    val task: Int,
    val part: String?,
    val remoteId: String?,
)

val Course.completedCount: Int get() = lessons.count { it.isCompleted }

val Course.currentLesson: LessonSummary? get() = lessons.firstOrNull { !it.isCompleted }

val LessonAudio.label: String
    get() = buildString {
        section?.let { append(it) }
        append(task)
        part?.let { append('.').append(it) }
    }

/**
 * An exercise from the book. Each kind asks something different of the learner,
 * so they are separate types rather than one shape with unused fields.
 */
sealed interface LessonExercise {
    val id: String
    val instruction: String
    val audioFile: String?

    /** Listen and read along; there is nothing to mark. */
    data class Repeat(
        override val id: String,
        override val instruction: String,
        override val audioFile: String?,
        val words: ImmutableList<String>,
    ) : LessonExercise

    /** Listen, then pick which of two near-identical words was said. */
    data class MinimalPair(
        override val id: String,
        override val instruction: String,
        override val audioFile: String?,
        val items: ImmutableList<MinimalPairItem>,
    ) : LessonExercise

    /**
     * Lines with words missing, typed in where they belong.
     *
     * Covers both shapes the book uses: a dialogue, where each line is somebody's
     * turn and the blanks fall mid-sentence, and a plain list of sentences. They
     * differ only in whether a line names a speaker.
     */
    data class GapFill(
        override val id: String,
        override val instruction: String,
        override val audioFile: String?,
        val items: ImmutableList<GapFillItem>,
    ) : LessonExercise

    /** Listen, then write down what was said. Nothing is given but the label. */
    data class Transcribe(
        override val id: String,
        override val instruction: String,
        override val audioFile: String?,
        val items: ImmutableList<TranscribeItem>,
    ) : LessonExercise

    /** Pair each item on the left with the one on the right that answers it. */
    data class Match(
        override val id: String,
        override val instruction: String,
        override val audioFile: String?,
        val items: ImmutableList<MatchItem>,
    ) : LessonExercise

    /** A word with letters missing, one cell apiece. */
    data class LetterFill(
        override val id: String,
        override val instruction: String,
        override val audioFile: String?,
        val items: ImmutableList<LetterFillItem>,
    ) : LessonExercise
}

data class MinimalPairItem(
    val label: String,
    val options: ImmutableList<String>,
    val answer: String,
)

/**
 * One line to complete.
 *
 * [speaker] is who says it, and is absent when the exercise is not a dialogue.
 * [prompt] carries [GAP_MARKER] where each word is missing, and [answers] are
 * those words in the order the markers appear.
 */
data class GapFillItem(
    val prompt: String,
    val answers: ImmutableList<String>,
    val speaker: String? = null,
)

data class TranscribeItem(
    val label: String,
    val answer: String,
)

/**
 * One pairing.
 *
 * [prompt] is either the text on the left or, written as [ICON_PREFIX] and a
 * name, a drawing standing in for it — the book pairs pictures with phrases as
 * often as it pairs sentences with answers.
 */
data class MatchItem(
    val label: String,
    val prompt: String,
    val answer: String,
) {
    val iconName: String? get() = prompt.removePrefix(ICON_PREFIX).takeIf { prompt.startsWith(ICON_PREFIX) }
}

/**
 * A word to complete a letter at a time.
 *
 * [pattern] is the word as the book prints it, with [LETTER_GAP] standing in for
 * each missing letter, and [letters] are those letters in order. The two are the
 * same length as the finished word, which is what lets the cells line up with it.
 */
data class LetterFillItem(
    val label: String,
    val pattern: String,
    val letters: ImmutableList<String>,
) {
    val answer: String get() = buildString {
        var next = 0
        pattern.forEach { character ->
            if (character == LETTER_GAP) append(letters.getOrElse(next++) { "" }) else append(character)
        }
    }
}

/** Where a word is missing from a line. */
const val GAP_MARKER = "___"

/** Where a single letter is missing from a word. */
const val LETTER_GAP = '_'

/** Marks a match prompt as a drawing rather than words. */
const val ICON_PREFIX = "icon:"

val LessonExercise.questionCount: Int
    get() = when (this) {
        is LessonExercise.Repeat -> words.size
        is LessonExercise.MinimalPair -> items.size
        is LessonExercise.GapFill -> items.sumOf { it.answers.size }
        is LessonExercise.Transcribe -> items.size
        is LessonExercise.Match -> items.size
        is LessonExercise.LetterFill -> items.sumOf { it.letters.size }
    }
