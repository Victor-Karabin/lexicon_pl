package com.lexicon.presentation.common

import androidx.lifecycle.SavedStateHandle

const val TRAINING_WORDS_ARG = "words"

private const val SEPARATOR = ","

fun SavedStateHandle.trainingVocabularyIds(): List<Long> =
    get<String>(TRAINING_WORDS_ARG)
        .orEmpty()
        .split(SEPARATOR)
        .mapNotNull { it.trim().toLongOrNull() }

fun List<Long>.asTrainingWordsArgument(): String = joinToString(SEPARATOR)
