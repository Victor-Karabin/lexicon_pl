package com.lexicon.model.vocabulary

enum class PartOfSpeech(val tag: String) {
    NOUN("n"),
    VERB("v"),
    ADJECTIVE("adj"),
    ADVERB("adv"),
    PREPOSITION("prep"),
    CONJUNCTION("conj"),
    PRONOUN("prn"),
    NUMERAL("num"),
    PARTICLE("part"),
    INTERJECTION("interj"),
    PHRASE("expr"),
    ;

    companion object {
        fun ofTag(tag: String?): PartOfSpeech? = entries.firstOrNull { it.tag == tag?.trim() }
    }
}

enum class Gender(val tag: String) {
    MASCULINE_PERSONAL("masculine personal"),
    MASCULINE_ANIMATE("masculine animate"),
    MASCULINE_INANIMATE("masculine inanimate"),
    FEMININE("feminine"),
    NEUTER("neuter"),
    PLURAL_ONLY("plural only"),
    ;

    companion object {
        fun ofTag(tag: String?): Gender? = entries.firstOrNull { it.tag == tag?.trim() }
    }
}

sealed interface WordForms {
    data class Noun(
        val gender: Gender,
        val plural: String?,
    ) : WordForms

    data class Adjective(
        val masculine: String,
        val feminine: String,
        val neuter: String,
        val pluralPersonal: String,
        val pluralOther: String,
    ) : WordForms
}
