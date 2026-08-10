package com.lexicon.presentation.common

import androidx.lifecycle.SavedStateHandle

/**
 * The optional word subset a training runs over.
 *
 * Trainings normally draw from the whole study set. A course lesson launches the
 * same screens restricted to its own words, and passes them as a route argument
 * so nothing between the lesson and the session use case has to know why.
 */
const val TRAINING_WORDS_ARG = "words"

private const val SEPARATOR = ","

fun SavedStateHandle.trainingVocabularyIds(): List<Long> =
    get<String>(TRAINING_WORDS_ARG)
        .orEmpty()
        .split(SEPARATOR)
        .mapNotNull { it.trim().toLongOrNull() }

fun List<Long>.asTrainingWordsArgument(): String = joinToString(SEPARATOR)
