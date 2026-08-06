package com.lexicon.domain.dictation

import com.lexicon.boundary.VocabularyItemBoundary

fun VocabularyItemBoundary.toWord(): Word =
    Word(
        id = id,
        text = text,
        translation = translation,
        transcription = transcription,
    )

/** Distinguishes single-word items from multi-word phrases/sentences, e.g. for distractor selection. */
val Word.isPhrase: Boolean get() = text.contains(' ')
