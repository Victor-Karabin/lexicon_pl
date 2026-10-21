package com.lexicon.data.repository

import com.lexicon.boundary.TranslationDirection
import com.lexicon.common.foldForSearch
import com.lexicon.data.local.VocabularySeeder
import com.lexicon.data.local.WordDao
import com.lexicon.data.local.WordEntity
import com.lexicon.data.local.searchKeyFor
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CorpusTranslatorImplTest {
    private val table = mutableListOf<WordEntity>()

    private val wordDao: WordDao = mockk {
        coEvery { search(any(), any(), any(), any(), any(), any()) } answers {
            val query = firstArg<String>()
            val limit = arg<Int>(4)
            val offset = arg<Int>(5)
            table.filter { it.searchKey.contains(query) }.sortedBy { it.text }.drop(offset).take(limit)
        }
        coEvery { withExactMatch(any(), any()) } answers {
            val query = firstArg<String>()
            table.filter { it.text.foldForSearch() == query || it.translation.foldForSearch() == query }
                .sortedBy { it.text }
                .take(secondArg())
        }
    }
    private val seeder: VocabularySeeder = mockk { coEvery { ensureSeeded() } returns Unit }
    private val translator = CorpusTranslatorImpl(wordDao, seeder)

    private fun add(
        id: Long,
        text: String,
        translation: String,
    ) = table.add(WordEntity(id, text, translation, "", searchKey = searchKeyFor(text, translation)))

    private fun crowdWith(
        needle: String,
        count: Int = 40,
    ) = (1..count).forEach { add(it.toLong(), "a$needle${it.toString().padStart(2, '0')}", "$needle gloss $it") }

    @Test
    fun `a Polish word that dozens of other entries contain still finds its own entry`() =
        runTest {
            crowdWith("ser")
            add(100, "ser", "cheese")

            assertEquals("cheese", translator.translate("ser", TranslationDirection.PL_TO_EN))
        }

    @Test
    fun `an English word that dozens of other glosses contain still finds its own entry`() =
        runTest {
            (1..40).forEach { add(it.toLong(), "a${it.toString().padStart(2, '0')}", "cheese pie $it") }
            add(100, "ser", "cheese")

            assertEquals("ser", translator.translate("cheese", TranslationDirection.EN_TO_PL))
        }

    @Test
    fun `the exact entry is found whatever the accents and case of the query`() =
        runTest {
            crowdWith("kosc")
            add(100, "kość", "bone")

            assertEquals("bone", translator.translate("KOSC", TranslationDirection.PL_TO_EN))
        }

    @Test
    fun `a query that only appears inside other entries has no suggestion`() =
        runTest {
            crowdWith("ser")

            assertNull(translator.translate("ser", TranslationDirection.PL_TO_EN))
        }

    @Test
    fun `a blank query is no suggestion`() =
        runTest {
            crowdWith("ser")

            assertNull(translator.translate("  ", TranslationDirection.PL_TO_EN))
        }
}
