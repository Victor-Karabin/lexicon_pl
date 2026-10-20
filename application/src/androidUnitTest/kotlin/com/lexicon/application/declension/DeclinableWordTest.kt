package com.lexicon.application.declension

import com.lexicon.interactors.declension.DeclensionColumn
import com.lexicon.interactors.declension.DeclinableWord
import com.lexicon.model.vocabulary.CaseForms
import com.lexicon.model.vocabulary.Gender
import com.lexicon.model.vocabulary.GrammaticalCase
import com.lexicon.model.vocabulary.PartOfSpeech
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import com.lexicon.model.vocabulary.WordForms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeclinableWordTest {
    private fun noun(
        text: String,
        gender: Gender,
        cell: (GrammaticalCase) -> CaseForms,
    ) = Word(
        id = VocabularyId(1),
        text = text,
        translation = "",
        transcription = "",
        partOfSpeech = PartOfSpeech.NOUN,
        forms = WordForms.Noun(gender, GrammaticalCase.entries.associateWith(cell)),
    )

    @Test
    fun `a plural-only noun stored in the singular cells is asked as plural`() {
        val dudy = DeclinableWord.of(noun("dudy", Gender.PLURAL_ONLY) { CaseForms("dudy", null) })!!
        assertEquals(listOf(DeclensionColumn.PLURAL), dudy.columns)
    }

    @Test
    fun `a plural-only noun stored in both columns is asked once`() {
        val kalorie = DeclinableWord.of(noun("kalorie", Gender.PLURAL_ONLY) { CaseForms("kalorie", "kalorie") })!!
        assertEquals(listOf(DeclensionColumn.PLURAL), kalorie.columns)
    }

    @Test
    fun `a word whose table declines a different headword is left out`() {
        assertNull(DeclinableWord.of(noun("uwaga krytyczna", Gender.FEMININE) { CaseForms("uwaga", "uwagi") }))

        val wykonczyc = Word(
            id = VocabularyId(2),
            text = "wykończyć",
            translation = "",
            transcription = "",
            partOfSpeech = PartOfSpeech.ADJECTIVE,
            forms = WordForms.Adjective("wykończony", "wykończona", "wykończone", "wykończeni", "wykończone"),
        )
        assertNull(DeclinableWord.of(wykonczyc))
    }
}
