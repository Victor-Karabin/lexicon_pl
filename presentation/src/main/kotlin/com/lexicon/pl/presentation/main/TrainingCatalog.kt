package com.lexicon.pl.presentation.main

/** Canonical training identifiers, shared with the app module's navigation routes. */
object TrainingIds {
    const val DICTATION = "dictation"
    const val DICTATION_PUZZLE = "dictation_puzzle"
    const val WORD_BUILDER = "word_builder"
    const val TRUE_OR_FALSE = "true_or_false"
    const val WORD_MATCH = "word_match"
    const val PRONUNCIATION_CHECK = "pronunciation_check"
}

data class TrainingCatalogEntry(val id: String, val displayName: String, val isEnabled: Boolean)

/** Names match "Software Development Specification" §9. */
val trainingCatalog =
    listOf(
        TrainingCatalogEntry(TrainingIds.DICTATION, "Dictation", isEnabled = true),
        TrainingCatalogEntry(TrainingIds.DICTATION_PUZZLE, "Dictation Puzzle", isEnabled = true),
        TrainingCatalogEntry("puzzle", "Puzzle", isEnabled = false),
        TrainingCatalogEntry("image_test", "Image Test", isEnabled = false),
        TrainingCatalogEntry(TrainingIds.WORD_MATCH, "Word Match", isEnabled = true),
        TrainingCatalogEntry(TrainingIds.TRUE_OR_FALSE, "True or False", isEnabled = true),
        TrainingCatalogEntry(TrainingIds.PRONUNCIATION_CHECK, "Pronunciation Check", isEnabled = true),
        TrainingCatalogEntry("memory_cards", "Memory Cards", isEnabled = false),
        TrainingCatalogEntry("crossword", "Crossword", isEnabled = false),
        TrainingCatalogEntry(TrainingIds.WORD_BUILDER, "Word Builder", isEnabled = true),
        TrainingCatalogEntry("mix", "Mix", isEnabled = false),
        TrainingCatalogEntry("custom_builder", "Custom Builder", isEnabled = false),
    )
