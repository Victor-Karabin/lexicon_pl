package com.lexicon.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lexicon.common.foldForSearch
import com.lexicon.model.vocabulary.CefrLevel
import com.lexicon.model.vocabulary.Gender
import com.lexicon.model.vocabulary.PartOfSpeech
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import com.lexicon.model.vocabulary.WordForms
import com.lexicon.model.vocabulary.WordStatus

@Entity(tableName = "words", indices = [Index("status")])
data class WordEntity(
    @PrimaryKey val id: Long,
    val text: String,
    val translation: String,
    val transcription: String,
    val status: String = WordStatus.UNDEFINED.name,
    val searchKey: String = "",
    val cefr: String = "",
    val example: String = "",
    val isDeleted: Boolean = false,
    val isUserCreated: Boolean = false,
    val picture: String? = null,
    val partOfSpeech: String = "",
    val gender: String = "",
    val plural: String = "",
    val adjectiveForms: String = "",
)

fun nextUserWordId(lowestExistingId: Long?): Long = minOf(lowestExistingId ?: 0L, 0L) - 1

fun searchKeyFor(
    text: String,
    translation: String,
): String = "${text.foldForSearch()} ${translation.foldForSearch()}"

fun WordEntity.toWord(): Word =
    Word(
        id = VocabularyId(id),
        text = text,
        translation = translation,
        transcription = transcription,
        status = WordStatus.ofName(status),
        cefr = CefrLevel.ofName(cefr.ifEmpty { null }),
        example = example,
        picture = picture,
        partOfSpeech = PartOfSpeech.ofTag(partOfSpeech),
        forms = formsOf(partOfSpeech = partOfSpeech, gender = gender, plural = plural, adjectiveForms = adjectiveForms),
    )

private const val FORM_SEPARATOR = '|'

private const val ADJECTIVE_FORM_COUNT = 5

fun List<String>.joinForms(): String = joinToString(FORM_SEPARATOR.toString())

private fun formsOf(
    partOfSpeech: String,
    gender: String,
    plural: String,
    adjectiveForms: String,
): WordForms? =
    when (PartOfSpeech.ofTag(partOfSpeech)) {
        PartOfSpeech.NOUN -> Gender.ofTag(gender)?.let { WordForms.Noun(it, plural.ifBlank { null }) }
        PartOfSpeech.ADJECTIVE ->
            adjectiveForms
                .split(FORM_SEPARATOR)
                .takeIf { forms -> forms.size == ADJECTIVE_FORM_COUNT && forms.all { it.isNotBlank() } }
                ?.let { WordForms.Adjective(it[0], it[1], it[2], it[3], it[4]) }

        else -> null
    }
