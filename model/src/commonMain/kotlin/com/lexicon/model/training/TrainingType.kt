package com.lexicon.model.training

private const val ONE_WORD_PER_STEP = 1

enum class TrainingType(
    val id: String,
    val minimumWords: Int,
) {
    DICTATION("dictation", ONE_WORD_PER_STEP),
    DICTATION_PUZZLE("dictation_puzzle", ONE_WORD_PER_STEP),
    TRUE_OR_FALSE("true_or_false", 2),
    WORD_MATCH("word_match", 4),
    PRONUNCIATION_CHECK("pronunciation_check", ONE_WORD_PER_STEP),
    PRONUNCIATION_SENTENCES("pronunciation_sentences", ONE_WORD_PER_STEP),
    PUZZLE("puzzle", ONE_WORD_PER_STEP),
    IMAGE_TEST("image_test", 6),
    MEMORY_CARDS("memory_cards", 6),
    MIX("mix", 6),
    CROSSWORD("crossword", 8),
    WORD_CARD("word_card", ONE_WORD_PER_STEP),
    PASSAGE_WRITE("passage_write", ONE_WORD_PER_STEP),
    PASSAGE_BANK("passage_bank", ONE_WORD_PER_STEP),
    FILLWORD("fillword", ONE_WORD_PER_STEP),
    CONJUGATION("conjugation", ONE_WORD_PER_STEP),
    ;

    companion object {
        fun ofId(id: String): TrainingType? = entries.firstOrNull { it.id == id }
    }
}
