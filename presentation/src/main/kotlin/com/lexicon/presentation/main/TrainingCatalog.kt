package com.lexicon.presentation.main

/** Canonical training identifiers, shared with the app module's navigation routes. */
object TrainingIds {
    const val DICTATION = "dictation"
    const val DICTATION_PUZZLE = "dictation_puzzle"
    const val TRUE_OR_FALSE = "true_or_false"
    const val WORD_MATCH = "word_match"
    const val PRONUNCIATION_CHECK = "pronunciation_check"
    const val PUZZLE = "puzzle"
    const val IMAGE_TEST = "image_test"
    const val MEMORY_CARDS = "memory_cards"
    const val MIX = "mix"
    const val CROSSWORD = "crossword"
}

data class TrainingCatalogEntry(val id: String, val displayName: String, val isEnabled: Boolean)

/** Names match "Software Development Specification" §9. */
val trainingCatalog =
    listOf(
        TrainingCatalogEntry(TrainingIds.DICTATION, "Dictation", isEnabled = true),
        TrainingCatalogEntry(TrainingIds.DICTATION_PUZZLE, "Dictation Puzzle", isEnabled = true),
        TrainingCatalogEntry(TrainingIds.PUZZLE, "Puzzle", isEnabled = true),
        TrainingCatalogEntry(TrainingIds.IMAGE_TEST, "Image Test", isEnabled = true),
        TrainingCatalogEntry(TrainingIds.WORD_MATCH, "Word Match", isEnabled = true),
        TrainingCatalogEntry(TrainingIds.TRUE_OR_FALSE, "True or False", isEnabled = true),
        TrainingCatalogEntry(TrainingIds.PRONUNCIATION_CHECK, "Pronunciation Check", isEnabled = true),
        TrainingCatalogEntry(TrainingIds.MEMORY_CARDS, "Memory Cards", isEnabled = true),
        TrainingCatalogEntry(TrainingIds.CROSSWORD, "Crossword", isEnabled = true),
        TrainingCatalogEntry(TrainingIds.MIX, "Mix", isEnabled = true),
        TrainingCatalogEntry("custom_builder", "Custom Builder", isEnabled = false),
    )

/** Falls back to the id so an unlisted route still gets a heading rather than a blank one. */
fun trainingDisplayName(id: String): String = trainingCatalog.firstOrNull { it.id == id }?.displayName ?: id
