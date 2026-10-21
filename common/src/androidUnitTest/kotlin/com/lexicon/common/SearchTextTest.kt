package com.lexicon.common

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchTextTest {
    @Test
    fun `every Polish diacritic folds to its base letter`() {
        assertEquals("acelnoszz", "ąćęłńóśźż".foldForSearch())
    }

    @Test
    fun `folding lowercases, so an upper-case query still matches`() {
        assertEquals("zolw", "ŻÓŁW".foldForSearch())
    }

    @Test
    fun `surrounding spaces are dropped`() {
        assertEquals("kot", "  kot  ".foldForSearch())
    }

    @Test
    fun `spaces inside a phrase are kept, so multi-word entries stay searchable`() {
        assertEquals("dzien dobry", "Dzień dobry".foldForSearch())
    }

    @Test
    fun `plain English text is unchanged apart from case`() {
        assertEquals("mineral water", "Mineral Water".foldForSearch())
    }

    @Test
    fun `folding is idempotent, so folding a stored key again cannot drift`() {
        val once = "Żółw".foldForSearch()

        assertEquals(once, once.foldForSearch())
    }

    @Test
    fun `an empty string folds to empty rather than failing`() {
        assertEquals("", "".foldForSearch())
    }

    @Test
    fun `accents that are not Polish stay in the plain fold`() {
        assertEquals("côte d'ivoire", "Côte d'Ivoire".foldForSearch())
    }

    @Test
    fun `the accent fold also drops other Latin accents and keeps the Polish fold`() {
        assertEquals("cote d'ivoire", "Côte d'Ivoire".foldAccentsForSearch())
        assertEquals("sao tome and principe", "São Tomé and Príncipe".foldAccentsForSearch())
        assertEquals("zolw", "ŻÓŁW".foldAccentsForSearch())
        assertEquals("brasilia", "Brasília".foldAccentsForSearch())
    }

    @Test
    fun `the accent fold is idempotent and trims`() {
        val once = "  Reykjavík ".foldAccentsForSearch()

        assertEquals("reykjavik", once)
        assertEquals(once, once.foldAccentsForSearch())
    }
}
