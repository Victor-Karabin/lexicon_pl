package com.lexicon.application.declension

import com.lexicon.boundary.DeclensionProgressBoundary
import com.lexicon.boundary.DeclensionRepository
import com.lexicon.boundary.ImageProvider
import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.declension.DECLENSION_COURSE_ID
import com.lexicon.interactors.declension.DeclensionColumn
import com.lexicon.interactors.declension.SubmitDeclensionAnswerRequest
import com.lexicon.model.vocabulary.CaseForms
import com.lexicon.model.vocabulary.Gender
import com.lexicon.model.vocabulary.GrammaticalCase
import com.lexicon.model.vocabulary.PartOfSpeech
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import com.lexicon.model.vocabulary.WordForms
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DeclensionUseCasesImplTest {
    private val kot = Word(
        id = VocabularyId(1),
        text = "kot",
        translation = "cat",
        transcription = "kɔt",
        partOfSpeech = PartOfSpeech.NOUN,
        forms = WordForms.Noun(
            Gender.MASCULINE_ANIMATE,
            mapOf(
                GrammaticalCase.NOMINATIVE to CaseForms("kot", "koty"),
                GrammaticalCase.GENITIVE to CaseForms("kota", "kotów"),
                GrammaticalCase.DATIVE to CaseForms("kotu", "kotom"),
                GrammaticalCase.ACCUSATIVE to CaseForms("kota", "koty"),
                GrammaticalCase.INSTRUMENTAL to CaseForms("kotem", "kotami"),
                GrammaticalCase.LOCATIVE to CaseForms("kocie", "kotach"),
                GrammaticalCase.VOCATIVE to CaseForms("kocie", "koty"),
            ),
        ),
    )

    private val nozyczki = Word(
        id = VocabularyId(2),
        text = "nożyczki",
        translation = "scissors",
        transcription = "",
        partOfSpeech = PartOfSpeech.NOUN,
        forms = WordForms.Noun(
            Gender.PLURAL_ONLY,
            GrammaticalCase.entries.associateWith { CaseForms(null, "nożyczki") },
        ),
    )

    private class FakeRepository(
        val words: List<Word>,
    ) : DeclensionRepository {
        val attempts = mutableListOf<DeclensionProgressBoundary>()

        override suspend fun declinableWords(
            foldedQuery: String,
            limit: Int,
            offset: Int,
        ): List<Word> = words.drop(offset).take(limit)

        override suspend fun studySetWords(): List<Word> = words

        override suspend fun progress(courseId: String): List<DeclensionProgressBoundary> = attempts

        override suspend fun recordAttempt(
            courseId: String,
            word: String,
            column: String,
            case: String,
            isCorrect: Boolean,
        ) {
            val old = attempts.firstOrNull { it.word == word && it.column == column && it.case == case }
            attempts.remove(old)
            attempts += DeclensionProgressBoundary(
                word = word,
                column = column,
                case = case,
                attempted = (old?.attempted ?: 0) + 1,
                correct = (old?.correct ?: 0) + if (isCorrect) 1 else 0,
                incorrect = (old?.incorrect ?: 0) + if (isCorrect) 0 else 1,
                streak = if (isCorrect) (old?.streak ?: 0) + 1 else 0,
            )
        }
    }

    private val firstOfTies = object : Random() {
        override fun nextBits(bitCount: Int): Int = 0
    }

    private val noImages = object : ImageProvider {
        override suspend fun searchImage(query: String): String? = null

        override suspend fun searchImages(
            query: String,
            count: Int,
            skip: Int,
        ): List<String> = emptyList()

        override suspend fun pinImage(
            query: String,
            imageUrl: String,
        ) = Unit
    }

    @Test
    fun `a noun is asked one number at a time, every case of it`() =
        runTest {
            val table = NextDeclensionTableUseCaseImpl(FakeRepository(listOf(kot)), noImages)(DECLENSION_COURSE_ID, random = firstOfTies)!!
            assertEquals(DeclensionColumn.SINGULAR, table.column)
            assertEquals(GrammaticalCase.entries, table.steps.map { it.variant.case })
            assertEquals("kocie", table.steps.last().spokenForm)
        }

    @Test
    fun `a plural-only noun is asked only in the plural`() =
        runTest {
            val table = NextDeclensionTableUseCaseImpl(FakeRepository(listOf(nozyczki)), noImages)(DECLENSION_COURSE_ID)!!
            assertEquals(DeclensionColumn.PLURAL, table.column)
        }

    @Test
    fun `the least practised table comes next, and a mastered one never`() =
        runTest {
            val repository = FakeRepository(listOf(kot))
            val next = NextDeclensionTableUseCaseImpl(repository, noImages)
            val submit = SubmitDeclensionAnswerUseCaseImpl(repository)

            repeat(2) {
                val singular = next(DECLENSION_COURSE_ID, random = firstOfTies)!!
                assertEquals(DeclensionColumn.SINGULAR, singular.column)
                submit(
                    SubmitDeclensionAnswerRequest(
                        DECLENSION_COURSE_ID,
                        singular,
                        singular.steps.associate { it.variant.case to it.spokenForm },
                    ),
                )
                if (it == 0) {
                    val plural = next(DECLENSION_COURSE_ID, random = firstOfTies)!!
                    assertEquals(DeclensionColumn.PLURAL, plural.column)
                    submit(SubmitDeclensionAnswerRequest(DECLENSION_COURSE_ID, plural, emptyMap()))
                }
            }
            assertEquals(DeclensionColumn.PLURAL, next(DECLENSION_COURSE_ID, random = firstOfTies)!!.column)
        }

    @Test
    fun `a word already asked this session waits while another can be asked`() =
        runTest {
            val repository = FakeRepository(listOf(kot, nozyczki))
            val next = NextDeclensionTableUseCaseImpl(repository, noImages)
            val practised = next(DECLENSION_COURSE_ID, asked = setOf("kot"))!!
            SubmitDeclensionAnswerUseCaseImpl(repository)(SubmitDeclensionAnswerRequest(DECLENSION_COURSE_ID, practised, emptyMap()))

            assertEquals("nożyczki", next(DECLENSION_COURSE_ID, asked = setOf("kot"))!!.word)
        }

    @Test
    fun `once every word has been asked, the session goes on with the least practised`() =
        runTest {
            val table = NextDeclensionTableUseCaseImpl(FakeRepository(listOf(kot)), noImages)(DECLENSION_COURSE_ID, asked = setOf("kot"))

            assertEquals("kot", table?.word)
        }

    @Test
    fun `answers are judged case by case and every attempt is recorded`() =
        runTest {
            val repository = FakeRepository(listOf(kot))
            val table = NextDeclensionTableUseCaseImpl(repository, noImages)(DECLENSION_COURSE_ID)!!
            val answers = table.steps.associate { it.variant.case to it.spokenForm } + (GrammaticalCase.LOCATIVE to "kotu")

            val response =
                SubmitDeclensionAnswerUseCaseImpl(repository)(SubmitDeclensionAnswerRequest(DECLENSION_COURSE_ID, table, answers))

            assertEquals(AnswerVerdict.CORRECT, response.verdicts[GrammaticalCase.GENITIVE])
            assertTrue(response.verdicts[GrammaticalCase.LOCATIVE] != AnswerVerdict.CORRECT)
            assertEquals(7, repository.attempts.size)
        }

    @Test
    fun `homographs in the study set are one word of the course`() =
        runTest {
            val course = LoadDeclensionCoursesUseCaseImpl(FakeRepository(listOf(kot, kot.copy(id = VocabularyId(3)))))().single()
            assertEquals(listOf("kot"), course.words)
            assertEquals(14, course.progress.total)
        }

    @Test
    fun `the course lists the study set, and there is none without words`() =
        runTest {
            val course = LoadDeclensionCoursesUseCaseImpl(FakeRepository(listOf(kot, nozyczki)))().single()
            assertEquals(listOf("kot", "nożyczki"), course.words)
            assertEquals(14 + 7, course.progress.total)
            assertTrue(LoadDeclensionCoursesUseCaseImpl(FakeRepository(emptyList()))().isEmpty())
            assertNull(NextDeclensionTableUseCaseImpl(FakeRepository(emptyList()), noImages)(DECLENSION_COURSE_ID))
        }
}
