package com.lexicon.interactors.course

import kotlinx.collections.immutable.ImmutableList

sealed interface LessonExercise {
    val id: String
    val instruction: String
    val audioFile: String?

    data class Repeat(
        override val id: String,
        override val instruction: String,
        override val audioFile: String?,
        val words: ImmutableList<String>,
    ) : LessonExercise

    data class MinimalPair(
        override val id: String,
        override val instruction: String,
        override val audioFile: String?,
        val items: ImmutableList<MinimalPairItem>,
    ) : LessonExercise

    data class GapFill(
        override val id: String,
        override val instruction: String,
        override val audioFile: String?,
        val items: ImmutableList<GapFillItem>,
    ) : LessonExercise

    data class Transcribe(
        override val id: String,
        override val instruction: String,
        override val audioFile: String?,
        val items: ImmutableList<TranscribeItem>,
    ) : LessonExercise

    data class Match(
        override val id: String,
        override val instruction: String,
        override val audioFile: String?,
        val items: ImmutableList<MatchItem>,
    ) : LessonExercise

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

data class GapFillItem(
    val prompt: String,
    val answers: ImmutableList<String>,
    val speaker: String? = null,
)

data class TranscribeItem(
    val label: String,
    val answer: String,
)

data class MatchItem(
    val label: String,
    val prompt: String,
    val answer: String,
) {
    val iconName: String? get() = prompt.removePrefix(ICON_PREFIX).takeIf { prompt.startsWith(ICON_PREFIX) }
}

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

const val GAP_MARKER = "___"

const val LETTER_GAP = '_'

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
