package com.lexicon.application.conjugation

import com.lexicon.boundary.ConjugationRepository
import com.lexicon.interactors.conjugation.ConjugationStep
import com.lexicon.interactors.conjugation.ConjugationTable
import com.lexicon.interactors.conjugation.ConjugationVariant
import com.lexicon.interactors.conjugation.GrammaticalPerson
import com.lexicon.interactors.conjugation.SubmitConjugationAnswerRequest
import com.lexicon.interactors.course.AnswerVerdict
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SubmitConjugationAnswerUseCaseImplTest {
    private val repository: ConjugationRepository = mockk(relaxed = true)
    private val submit = SubmitConjugationAnswerUseCaseImpl(repository)

    private fun step(
        person: GrammaticalPerson,
        vararg forms: String,
    ) = ConjugationStep(ConjugationVariant("mieszkać", person), persistentListOf(*forms))

    private val table = ConjugationTable(
        infinitive = "mieszkać",
        steps = persistentListOf(
            step(GrammaticalPerson.JA, "mieszkam"),
            step(GrammaticalPerson.TY, "mieszkasz"),
            step(GrammaticalPerson.ON_ONA_ONO, "mieszka"),
            step(GrammaticalPerson.ONI_ONE, "mieszkają"),
        ),
    )

    @Test
    fun `the whole form is checked ignoring case and spaces, and missing Polish letters are almost`() =
        runTest {
            val response = submit(
                SubmitConjugationAnswerRequest(
                    courseId = "course",
                    table = table,
                    answers = mapOf(
                        GrammaticalPerson.JA to " Mieszkam ",
                        GrammaticalPerson.TY to "sz",
                        GrammaticalPerson.ON_ONA_ONO to "mieszkaja",
                        GrammaticalPerson.ONI_ONE to "mieszkają.",
                    ),
                ),
            )

            assertEquals(
                mapOf(
                    GrammaticalPerson.JA to AnswerVerdict.CORRECT,
                    GrammaticalPerson.TY to AnswerVerdict.WRONG,
                    GrammaticalPerson.ON_ONA_ONO to AnswerVerdict.WRONG,
                    GrammaticalPerson.ONI_ONE to AnswerVerdict.CORRECT,
                ),
                response.verdicts,
            )
            assertFalse(response.allCorrect)
            coVerify { repository.recordAttempt("course", "mieszkać", "ja", true) }
            coVerify { repository.recordAttempt("course", "mieszkać", "ty", false) }
        }

    @Test
    fun `either form is right where the verb has two`() =
        runTest {
            val bajac = ConjugationTable(
                infinitive = "bajać",
                steps = persistentListOf(
                    ConjugationStep(ConjugationVariant("bajać", GrammaticalPerson.JA), persistentListOf("baję", "bajam")),
                ),
            )
            listOf("baję", "bajam").forEach { typed ->
                val response = submit(SubmitConjugationAnswerRequest("course", bajac, mapOf(GrammaticalPerson.JA to typed)))
                assertEquals(typed, AnswerVerdict.CORRECT, response.verdicts[GrammaticalPerson.JA])
            }
        }

    @Test
    fun `a form typed without its Polish letters is almost, and counts as a miss`() =
        runTest {
            val response = submit(
                SubmitConjugationAnswerRequest(
                    "course",
                    table.copy(steps = persistentListOf(table.steps[3])),
                    mapOf(GrammaticalPerson.ONI_ONE to "mieszkaja"),
                ),
            )

            assertEquals(AnswerVerdict.ALMOST, response.verdicts[GrammaticalPerson.ONI_ONE])
            coVerify { repository.recordAttempt("course", "mieszkać", "oni/one", false) }
        }
}
