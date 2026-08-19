package com.lexicon.application.conjugation

import com.lexicon.boundary.VerbConjugationBoundary
import com.lexicon.interactors.conjugation.ConjugationAnswerMode
import com.lexicon.interactors.conjugation.GrammaticalPerson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Exactly the values in conjugations.json, so the tests fail if the data is misread. */
private val BYC = VerbConjugationBoundary(
    "być",
    mapOf(
        "ja" to listOf("jestem"),
        "ty" to listOf("jesteś"),
        "on/ona/ono" to listOf("jest"),
        "my" to listOf("jesteśmy"),
        "wy" to listOf("jesteście"),
        "oni/one" to listOf("są"),
    ),
)

private val CHODZIC = VerbConjugationBoundary(
    "chodzić",
    mapOf(
        "ja" to listOf("chodzę"),
        "ty" to listOf("chodzisz"),
        "on/ona/ono" to listOf("chodzi"),
        "my" to listOf("chodzimy"),
        "wy" to listOf("chodzicie"),
        "oni/one" to listOf("chodzą"),
    ),
)

private val BRAC = VerbConjugationBoundary(
    "brać",
    mapOf(
        "ja" to listOf("biorę"),
        "ty" to listOf("bierzesz"),
        "on/ona/ono" to listOf("bierze"),
        "my" to listOf("bierzemy"),
        "wy" to listOf("bierzecie"),
        "oni/one" to listOf("biorą"),
    ),
)

private val BAC_SIE = VerbConjugationBoundary(
    "bać się",
    mapOf(
        "ja" to listOf("boję się"),
        "ty" to listOf("boisz się"),
        "on/ona/ono" to listOf("boi się"),
        "my" to listOf("boimy się"),
        "wy" to listOf("boicie się"),
        "oni/one" to listOf("boją się"),
    ),
)

private val BAWIC_SIE = VerbConjugationBoundary(
    "bawić się",
    mapOf(
        "ja" to listOf("bawię się"),
        "ty" to listOf("bawisz się"),
        "on/ona/ono" to listOf("bawi się"),
        "my" to listOf("bawimy się"),
        "wy" to listOf("bawicie się"),
        "oni/one" to listOf("bawią się"),
    ),
)

/** boleć is the partial case: only the third persons exist, the rest are N/A in source. */
private val BOLEC = VerbConjugationBoundary(
    "boleć",
    mapOf("on/ona/ono" to listOf("boli"), "oni/one" to listOf("bolą")),
)

/** bajać carries two accepted forms per person, separated by ';' in source. */
private val BAJAC = VerbConjugationBoundary(
    "bajać",
    mapOf(
        "ja" to listOf("baję", "bajam"),
        "ty" to listOf("bajesz", "bajasz"),
        "on/ona/ono" to listOf("baje", "baja"),
        "my" to listOf("bajemy", "bajamy"),
        "wy" to listOf("bajecie", "bajacie"),
        "oni/one" to listOf("bają", "bajają"),
    ),
)

private val EMPTY = VerbConjugationBoundary("beknąć", emptyMap())

class ConjugationTablesTest {
    private val pool = listOf(BYC, CHODZIC, BRAC, BAC_SIE, BAWIC_SIE, BOLEC, BAJAC).map { it.toVerb() }

    private fun verb(source: VerbConjugationBoundary) = source.toVerb()

    @Test
    fun `a verb with no forms is not teachable`() {
        assertTrue(!verb(EMPTY).isTeachable)
        assertTrue(verb(EMPTY).persons.isEmpty())
    }

    @Test
    fun `a partly filled verb offers only the persons it has`() {
        val bolec = verb(BOLEC)

        assertTrue(bolec.isTeachable)
        assertTrue(!bolec.isComplete)
        assertEquals(listOf(GrammaticalPerson.ON_ONA_ONO, GrammaticalPerson.ONI_ONE), bolec.persons)
    }

    @Test
    fun `an irregular verb falls back to whole forms rather than invented endings`() {
        val question = verb(BYC).step(GrammaticalPerson.JA, pool)

        assertNotNull(question)
        assertEquals(ConjugationAnswerMode.FULL_FORM, question!!.mode)
        assertEquals(listOf("jestem"), question.correctOptions)
    }

    @Test
    fun `a regular verb is asked by its ending`() {
        val question = verb(CHODZIC).step(GrammaticalPerson.JA, pool)!!

        assertEquals(ConjugationAnswerMode.ENDING, question.mode)
        assertEquals("chodz", question.stem)
        assertEquals(listOf("ę"), question.correctOptions)
        assertEquals("chodzę", question.spokenForm)
    }

    @Test
    fun `the stem comes from the data, not from the infinitive`() {
        val question = verb(BRAC).step(GrammaticalPerson.TY, pool)!!

        assertEquals(ConjugationAnswerMode.ENDING, question.mode)
        assertEquals("bi", question.stem)
        assertEquals(listOf("erzesz"), question.correctOptions)
        assertEquals("bi" + question.correctOptions.first(), "bierzesz")
    }

    @Test
    fun `a reflexive verb keeps sie in the answer`() {
        val question = verb(BAC_SIE).step(GrammaticalPerson.JA, pool)!!

        assertEquals("bo", question.stem)
        assertEquals(listOf("ję się"), question.correctOptions)
        assertEquals("boję się", question.spokenForm)
    }

    /** A stem stopping mid-gap would leave " się" as the ending, which teaches nothing. */
    @Test
    fun `a reflexive verb whose ending would be only sie is asked whole`() {
        val question = verb(BAWIC_SIE).step(GrammaticalPerson.ON_ONA_ONO, pool)!!

        assertEquals(ConjugationAnswerMode.FULL_FORM, question.mode)
        assertEquals(listOf("bawi się"), question.correctOptions)
    }

    @Test
    fun `both source variants are accepted where the data gives two`() {
        val question = verb(BAJAC).step(GrammaticalPerson.JA, pool)!!

        assertEquals(2, question.correctOptions.size)
        assertTrue(question.correctOptions.all { it.isNotBlank() })
    }

    @Test
    fun `the correct answer is always among the options`() {
        pool.forEach { verb ->
            verb.persons.forEach { person ->
                val question = verb.step(person, pool)!!
                assertTrue(
                    "${verb.infinitive} ${person.label}",
                    question.correctOptions.any { it in question.options },
                )
            }
        }
    }

    @Test
    fun `options never repeat`() {
        pool.forEach { verb ->
            verb.persons.forEach { person ->
                val options = verb.step(person, pool)!!.options
                assertEquals(verb.infinitive, options.size, options.distinct().size)
            }
        }
    }

    @Test
    fun `no option is ever blank`() {
        pool.forEach { verb ->
            verb.persons.forEach { person ->
                assertTrue(verb.step(person, pool)!!.options.none { it.isBlank() })
            }
        }
    }

    @Test
    fun `a distractor is never also a correct answer`() {
        pool.forEach { verb ->
            verb.persons.forEach { person ->
                val question = verb.step(person, pool)!!
                val wrong = question.options.filterNot { it in question.correctOptions }
                assertTrue(wrong.none { candidate -> question.correctOptions.any { it.equals(candidate, true) } })
            }
        }
    }

    @Test
    fun `a verb short of forms borrows distractors from the others`() {
        val question = verb(BOLEC).step(GrammaticalPerson.ON_ONA_ONO, pool)!!

        assertTrue("only ${question.options.size} options", question.options.size > 2)
    }

    @Test
    fun `a person the verb does not have yields no question`() {
        assertNull(verb(BOLEC).step(GrammaticalPerson.JA, pool))
    }

    @Test
    fun `an ending always rebuilds its form when joined to the stem`() {
        pool.forEach { verb ->
            verb.persons.forEach { person ->
                val question = verb.step(person, pool)!!
                if (question.mode == ConjugationAnswerMode.ENDING) {
                    assertTrue(
                        "${verb.infinitive} ${person.label}",
                        question.correctOptions.any { question.stem + it == question.spokenForm },
                    )
                }
            }
        }
    }
}
