package com.lexicon.application.conjugation

import com.lexicon.boundary.VerbConjugationBoundary
import com.lexicon.interactors.conjugation.GrammaticalPerson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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

private val BOLEC = VerbConjugationBoundary(
    "boleć",
    mapOf("on/ona/ono" to listOf("boli"), "oni/one" to listOf("bolą")),
)

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
        assertNull(verb(EMPTY).question())
    }

    @Test
    fun `a partly filled verb offers only the persons it has`() {
        val bolec = verb(BOLEC)

        assertTrue(bolec.isTeachable)
        assertTrue(!bolec.isComplete)
        assertEquals(listOf(GrammaticalPerson.ON_ONA_ONO, GrammaticalPerson.ONI_ONE), bolec.question()!!.steps.map { it.variant.person })
    }

    @Test
    fun `every person is asked as the whole form, regular or not`() {
        assertEquals(listOf("jestem"), verb(BYC).step(GrammaticalPerson.JA)!!.forms)
        assertEquals(listOf("chodzę"), verb(CHODZIC).step(GrammaticalPerson.JA)!!.forms)
        assertEquals(listOf("bierzesz"), verb(BRAC).step(GrammaticalPerson.TY)!!.forms)
    }

    @Test
    fun `a reflexive verb keeps sie in the answer`() {
        val step = verb(BAC_SIE).step(GrammaticalPerson.JA)!!

        assertEquals(listOf("boję się"), step.forms)
        assertEquals("boję się", step.spokenForm)
    }

    @Test
    fun `both source variants are accepted where the data gives two`() {
        val step = verb(BAJAC).step(GrammaticalPerson.JA)!!

        assertEquals(listOf("baję", "bajam"), step.forms)
        assertEquals("baję", step.spokenForm)
    }

    @Test
    fun `a person the verb does not have yields no question`() {
        assertNull(verb(BOLEC).step(GrammaticalPerson.JA))
    }

    @Test
    fun `a table asks each person once and never with a blank form`() {
        pool.forEach { verb ->
            val table = verb.question()!!
            assertEquals(verb.infinitive, verb.persons, table.steps.map { it.variant.person })
            assertTrue(verb.infinitive, table.steps.all { step -> step.forms.isNotEmpty() && step.forms.none { it.isBlank() } })
        }
    }
}
