package com.lexicon.application.presets

import com.lexicon.boundary.ConjugationRepository
import com.lexicon.boundary.VerbConjugationBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.conjugation.GrammaticalPerson
import com.lexicon.interactors.presets.WordGrammar
import com.lexicon.model.vocabulary.Gender
import com.lexicon.model.vocabulary.PartOfSpeech
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import com.lexicon.model.vocabulary.WordForms
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetWordGrammarUseCaseImplTest {
    private val vocabulary: VocabularyRepository = mockk()
    private val conjugations: ConjugationRepository = mockk()

    private val getWordGrammar = GetWordGrammarUseCaseImpl(vocabulary, conjugations)

    private fun stored(
        text: String,
        partOfSpeech: PartOfSpeech?,
        forms: WordForms? = null,
    ) = Word(VocabularyId(1), text, "gloss", "", partOfSpeech = partOfSpeech, forms = forms)

    @Test
    fun `a noun carries its gender and plural`() =
        runTest {
            coEvery { vocabulary.getWord(1) } returns
                stored("kot", PartOfSpeech.NOUN, WordForms.Noun(Gender.MASCULINE_ANIMATE, "koty"))

            assertEquals(WordGrammar.Noun(Gender.MASCULINE_ANIMATE, "koty"), getWordGrammar(VocabularyId(1)))
        }

    @Test
    fun `an adjective carries all five forms`() =
        runTest {
            val forms = WordForms.Adjective("dobry", "dobra", "dobre", "dobrzy", "dobre")
            coEvery { vocabulary.getWord(1) } returns stored("dobry", PartOfSpeech.ADJECTIVE, forms)

            assertEquals(WordGrammar.Adjective(forms), getWordGrammar(VocabularyId(1)))
        }

    @Test
    fun `a verb is given the conjugation the verb trainings already use`() =
        runTest {
            coEvery { vocabulary.getWord(1) } returns stored("mieć", PartOfSpeech.VERB)
            coEvery { conjugations.verbPage("mieć", any(), 0) } returns
                listOf(
                    VerbConjugationBoundary(infinitive = "mieć bardzo", forms = mapOf("ja" to listOf("x"))),
                    VerbConjugationBoundary(infinitive = "mieć", forms = mapOf("ja" to listOf("mam"))),
                )

            val grammar = getWordGrammar(VocabularyId(1)) as WordGrammar.Verb

            assertEquals("mieć", grammar.conjugation?.infinitive)
            assertEquals(listOf("mam"), grammar.conjugation?.formsFor(GrammaticalPerson.JA))
        }

    @Test
    fun `a verb with no conjugation on file still says it is a verb`() =
        runTest {
            coEvery { vocabulary.getWord(1) } returns stored("abakować", PartOfSpeech.VERB)
            coEvery { conjugations.verbPage(any(), any(), any()) } returns emptyList()

            assertEquals(WordGrammar.Verb(null), getWordGrammar(VocabularyId(1)))
        }

    @Test
    fun `a preposition has a part of speech and nothing else`() =
        runTest {
            coEvery { vocabulary.getWord(1) } returns stored("bez", PartOfSpeech.PREPOSITION)

            assertEquals(WordGrammar.Plain(PartOfSpeech.PREPOSITION), getWordGrammar(VocabularyId(1)))
        }

    @Test
    fun `a word of unknown kind, and a missing word, have no grammar`() =
        runTest {
            coEvery { vocabulary.getWord(1) } returns stored("własne", null)
            coEvery { vocabulary.getWord(2) } returns null

            assertNull(getWordGrammar(VocabularyId(1)))
            assertNull(getWordGrammar(VocabularyId(2)))
        }
}
