package com.lexicon.application.declension

import com.lexicon.interactors.declension.DeclensionColumn
import com.lexicon.interactors.declension.PolishAdjectives
import com.lexicon.model.vocabulary.GrammaticalCase
import com.lexicon.model.vocabulary.WordForms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PolishAdjectivesTest {
    private fun forms(vararg forms: String) = WordForms.Adjective(forms[0], forms[1], forms[2], forms[3], forms[4])

    private fun column(
        forms: WordForms.Adjective,
        column: DeclensionColumn,
    ): List<List<String>> = GrammaticalCase.entries.map { PolishAdjectives.decline(forms)!!.getValue(column).getValue(it) }

    @Test
    fun `a hard adjective takes -ego, -emu, -ym and -ej`() {
        val dobry = forms("dobry", "dobra", "dobre", "dobrzy", "dobre")
        assertEquals(
            listOf(
                listOf("dobry"),
                listOf("dobrego"),
                listOf("dobremu"),
                listOf("dobrego", "dobry"),
                listOf("dobrym"),
                listOf("dobrym"),
                listOf("dobry"),
            ),
            column(dobry, DeclensionColumn.MASCULINE),
        )
        assertEquals(
            listOf(
                listOf("dobra"),
                listOf("dobrej"),
                listOf("dobrej"),
                listOf("dobrą"),
                listOf("dobrą"),
                listOf("dobrej"),
                listOf("dobra"),
            ),
            column(dobry, DeclensionColumn.FEMININE),
        )
        assertEquals(
            listOf(
                listOf("dobre"),
                listOf("dobrego"),
                listOf("dobremu"),
                listOf("dobre"),
                listOf("dobrym"),
                listOf("dobrym"),
                listOf("dobre"),
            ),
            column(dobry, DeclensionColumn.NEUTER),
        )
    }

    @Test
    fun `the plural tells men from everything else only in the nominative and accusative`() {
        val dobry = forms("dobry", "dobra", "dobre", "dobrzy", "dobre")
        assertEquals(
            listOf(
                listOf("dobrzy"),
                listOf("dobrych"),
                listOf("dobrym"),
                listOf("dobrych"),
                listOf("dobrymi"),
                listOf("dobrych"),
                listOf("dobrzy"),
            ),
            column(dobry, DeclensionColumn.PLURAL_PERSONAL),
        )
        assertEquals(
            listOf(
                listOf("dobre"),
                listOf("dobrych"),
                listOf("dobrym"),
                listOf("dobre"),
                listOf("dobrymi"),
                listOf("dobrych"),
                listOf("dobre"),
            ),
            column(dobry, DeclensionColumn.PLURAL_OTHER),
        )
    }

    @Test
    fun `after k and g the ending keeps an i`() {
        val polski = forms("polski", "polska", "polskie", "polscy", "polskie")
        assertEquals(
            listOf("polskiego"),
            PolishAdjectives.decline(polski)!!.getValue(DeclensionColumn.MASCULINE).getValue(GrammaticalCase.GENITIVE),
        )
        assertEquals(
            listOf("polskiej"),
            PolishAdjectives.decline(polski)!!.getValue(DeclensionColumn.FEMININE).getValue(GrammaticalCase.GENITIVE),
        )
        assertEquals(
            listOf("polską"),
            PolishAdjectives.decline(polski)!!.getValue(DeclensionColumn.FEMININE).getValue(GrammaticalCase.ACCUSATIVE),
        )
        assertEquals(
            listOf("polskimi"),
            PolishAdjectives.decline(polski)!!.getValue(DeclensionColumn.PLURAL_OTHER).getValue(GrammaticalCase.INSTRUMENTAL),
        )

        val drogi = forms("drogi", "droga", "drogie", "drodzy", "drogie")
        assertEquals(
            listOf("drogiej"),
            PolishAdjectives.decline(drogi)!!.getValue(DeclensionColumn.FEMININE).getValue(GrammaticalCase.LOCATIVE),
        )
    }

    @Test
    fun `a soft adjective keeps its i throughout`() {
        val tani = forms("tani", "tania", "tanie", "tani", "tanie")
        val declined = PolishAdjectives.decline(tani)!!
        assertEquals(listOf("taniego"), declined.getValue(DeclensionColumn.MASCULINE).getValue(GrammaticalCase.GENITIVE))
        assertEquals(listOf("taniej"), declined.getValue(DeclensionColumn.FEMININE).getValue(GrammaticalCase.DATIVE))
        assertEquals(listOf("tanią"), declined.getValue(DeclensionColumn.FEMININE).getValue(GrammaticalCase.INSTRUMENTAL))
        assertEquals(listOf("tanich"), declined.getValue(DeclensionColumn.PLURAL_PERSONAL).getValue(GrammaticalCase.GENITIVE))
    }

    @Test
    fun `phrases and adjectives outside the -y, -i pattern are left out`() {
        assertNull(PolishAdjectives.decline(forms("pewny siebie", "pewna siebie", "pewne siebie", "pewni siebie", "pewne siebie")))
        assertNull(PolishAdjectives.decline(forms("pewien", "pewna", "pewne", "pewni", "pewne")))
        assertNull(PolishAdjectives.decline(forms("twój", "twa", "twe", "twoi", "twe")))
    }
}
