package com.lexicon.domain.dictation

import com.lexicon.boundary.VocabularyItemBoundary

fun VocabularyItemBoundary.toWord(): Word =
    Word(
        id = id,
        text = text,
        translation = translation,
        transcription = transcription,
    )

val Word.isPhrase: Boolean get() = text.contains(' ')
