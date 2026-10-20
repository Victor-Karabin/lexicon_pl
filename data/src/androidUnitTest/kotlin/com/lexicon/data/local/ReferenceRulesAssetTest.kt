package com.lexicon.data.local

import com.lexicon.model.rules.CASES_RULE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReferenceRulesAssetTest {
    private val rules = parseReferenceRules(File("src/androidMain/assets/$REFERENCE_RULES_ASSET_PATH").readText())

    @Test
    fun `the cases are a rule of their own`() {
        val cases = rules.single { it.id == CASES_RULE }
        assertEquals("Przypadki", cases.title)
        assertEquals(7, cases.sections.first().tables.single().rows.size)
    }

    @Test
    fun `rules have distinct ids, a title and sections`() {
        assertEquals(rules.size, rules.map { it.id }.toSet().size)
        rules.forEach { rule ->
            assertTrue(rule.id.value, rule.title.isNotBlank() && rule.sections.isNotEmpty())
            rule.sections.forEach { section ->
                assertTrue("${rule.id.value} ${section.title}", section.title.isNotBlank())
                assertTrue(
                    "${rule.id.value} ${section.title}",
                    section.intro.isNotEmpty() || section.tables.isNotEmpty() || section.notes.isNotEmpty(),
                )
            }
        }
    }

    @Test
    fun `every table row has a cell under each heading`() =
        rules.flatMap { rule -> rule.sections.map { "${rule.id.value} ${it.title}" to it } }.forEach { (where, section) ->
            section.tables.forEach { table ->
                table.rows.forEach { row -> assertEquals("$where $row", table.header.size, row.size) }
            }
        }
}
