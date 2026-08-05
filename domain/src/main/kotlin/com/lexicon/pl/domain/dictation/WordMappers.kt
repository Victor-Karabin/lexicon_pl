package com.lexicon.pl.domain.dictation

import com.lexicon.pl.boundary.VocabularyItemBoundary

fun VocabularyItemBoundary.toWord(): Word = Word(
    id = id,
    text = text,
    translation = translation,
    transcription = transcription,
)
