package com.lexicon.application.presets

import com.lexicon.boundary.ConjugationRepository
import com.lexicon.boundary.VerbConjugationBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.presets.FillWordGrammarUseCase
import com.lexicon.model.vocabulary.PartOfSpeech
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteWordUseCasesImplTest {
    private val vocabulary: VocabularyRepository = mockk(relaxed = true)
    private val conjugations: ConjugationRepository = mockk(relaxed = true)
    private val fillWordGrammar: FillWordGrammarUseCase = mockk(relaxed = true)
    private val appScope = TestScope()

    private val deleteWord = DeleteWordUseCaseImpl(vocabulary, conjugations)
    private val restoreWord = RestoreWordUseCaseImpl(vocabulary, conjugations, fillWordGrammar, appScope)

    private val kotkowac = Word(VocabularyId(-1), "kotkować", "to kitten", "", partOfSpeech = PartOfSpeech.VERB)

    @Test
    fun `deleting a word takes the conjugation written for it out of the verb list`() =
        runTest {
            coEvery { vocabulary.getWord(-1) } returns kotkowac

            deleteWord(VocabularyId(-1))

            coVerify { vocabulary.deleteWord(-1) }
            coVerify { conjugations.deleteUserVerb("kotkować") }
        }

    @Test
    fun `restoring a verb whose conjugation went with it writes the conjugation again`() =
        runTest {
            coEvery { vocabulary.getWord(-1) } returns kotkowac
            coEvery { conjugations.verb("kotkować") } returns null

            restoreWord(VocabularyId(-1))
            appScope.testScheduler.advanceUntilIdle()

            coVerify { vocabulary.restoreWord(-1) }
            coVerify { fillWordGrammar(VocabularyId(-1)) }
        }

    @Test
    fun `restoring a bundled verb, whose conjugation never left, asks for nothing`() =
        runTest {
            coEvery { vocabulary.getWord(1) } returns kotkowac.copy(id = VocabularyId(1), text = "mieć")
            coEvery { conjugations.verb("mieć") } returns VerbConjugationBoundary("mieć", mapOf("ja" to listOf("mam")))

            restoreWord(VocabularyId(1))
            appScope.testScheduler.advanceUntilIdle()

            coVerify(exactly = 0) { fillWordGrammar(any()) }
        }

    @Test
    fun `restoring a word that is not a verb asks for nothing`() =
        runTest {
            coEvery { vocabulary.getWord(1) } returns kotkowac.copy(partOfSpeech = PartOfSpeech.NOUN)

            restoreWord(VocabularyId(1))
            appScope.testScheduler.advanceUntilIdle()

            coVerify(exactly = 0) { fillWordGrammar(any()) }
        }
}
