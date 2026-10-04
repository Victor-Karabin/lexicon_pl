package com.lexicon.data.local

import kotlinx.serialization.Serializable

@Serializable
data class VocabularySeedItem(
    val id: Long,
    val text: String,
    val translation: String,
    val transcription: String,
    val cefr: String = "",
    val example: String = "",
    val picture: String? = null,
    val partOfSpeech: String = "",
    val gender: String = "",
    val declension: String = "",
    val forms: List<String> = emptyList(),
)

fun VocabularySeedItem.toEntity(): WordEntity =
    WordEntity(
        id = id,
        text = text,
        translation = translation,
        transcription = transcription,
        searchKey = searchKeyFor(text, translation),
        cefr = cefr,
        example = example,
        picture = picture,
        partOfSpeech = partOfSpeech,
        gender = gender,
        declension = declension,
        adjectiveForms = forms.joinForms(),
    )
