package com.lexicon.application.presets

import com.lexicon.boundary.ConjugationRepository
import com.lexicon.boundary.GeneratedGrammarBoundary
import com.lexicon.boundary.VerbConjugationBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.boundary.WordGrammarGenerator
import com.lexicon.model.vocabulary.CaseForms
import com.lexicon.model.vocabulary.Gender
import com.lexicon.model.vocabulary.GrammaticalCase
import com.lexicon.model.vocabulary.PartOfSpeech
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import com.lexicon.model.vocabulary.WordForms
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FillWordGrammarUseCaseImplTest {
    private val vocabulary: VocabularyRepository = mockk(relaxed = true)
    private val conjugations: ConjugationRepository = mockk(relaxed = true)
    private val generator: WordGrammarGenerator = mockk()

    private val fillWordGrammar = FillWordGrammarUseCaseImpl(vocabulary, conjugations, generator)

    private val kotek = Word(VocabularyId(-1), "kotek", "kitten", "")

    @Test
    fun `a new noun is given the gender and cases that were written for it`() =
        runTest {
            val forms =
                WordForms.Noun(
                    Gender.MASCULINE_ANIMATE,
                    mapOf(GrammaticalCase.NOMINATIVE to CaseForms("kotek", "kotki")),
                )
            coEvery { vocabulary.getWord(-1) } returns kotek
            coEvery { generator.generate("kotek", "kitten") } returns
                GeneratedGrammarBoundary(PartOfSpeech.NOUN, forms)

            fillWordGrammar(VocabularyId(-1))

            coVerify { vocabulary.setGrammar(-1, PartOfSpeech.NOUN, forms) }
            coVerify(exactly = 0) { conjugations.saveVerb(any()) }
        }

    @Test
    fun `a new verb's conjugation is filed where the verb trainings read it`() =
        runTest {
            coEvery { vocabulary.getWord(-1) } returns kotek.copy(text = "kotkować", translation = "to kitten")
            coEvery { generator.generate(any(), any()) } returns
                GeneratedGrammarBoundary(PartOfSpeech.VERB, conjugation = mapOf("ja" to listOf("kotkuję")))

            fillWordGrammar(VocabularyId(-1))

            coVerify {
                conjugations.saveVerb(
                    VerbConjugationBoundary(
                        infinitive = "kotkować",
                        forms = mapOf("ja" to listOf("kotkuję")),
                        translation = "to kitten",
                        example = "",
                    ),
                )
            }
        }

    @Test
    fun `nothing is written when the grammar could not be generated`() =
        runTest {
            coEvery { vocabulary.getWord(-1) } returns kotek
            coEvery { generator.generate(any(), any()) } returns null

            fillWordGrammar(VocabularyId(-1))

            coVerify(exactly = 0) { vocabulary.setGrammar(any(), any(), any()) }
        }

    @Test
    fun `a generator that fails leaves the word as it was`() =
        runTest {
            coEvery { vocabulary.getWord(-1) } returns kotek
            coEvery { generator.generate(any(), any()) } throws IllegalStateException("no network")

            fillWordGrammar(VocabularyId(-1))

            coVerify(exactly = 0) { vocabulary.setGrammar(any(), any(), any()) }
        }
}
