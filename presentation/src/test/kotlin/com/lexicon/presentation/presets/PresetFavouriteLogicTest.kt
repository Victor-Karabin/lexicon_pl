package com.lexicon.presentation.presets

import com.lexicon.interactors.presets.LocalizedText
import com.lexicon.interactors.presets.PresetCategory
import com.lexicon.interactors.presets.PresetFavouriteState
import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.PresetWord
import com.lexicon.interactors.presets.VocabularyId
import com.lexicon.interactors.presets.VocabularyPreset
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

class PresetFavouriteLogicTest {
    private fun preset(vararg ids: Long) =
        VocabularyPreset(
            id = PresetId("food"),
            title = LocalizedText(mapOf("en" to "Food")),
            description = LocalizedText(mapOf("en" to "")),
            category = PresetCategory("everyday-life", 3, LocalizedText(mapOf("en" to "Everyday life"))),
            icon = null,
            color = null,
            popularity = 1,
            estimatedDuration = 5.minutes,
            vocabularyIds = ids.map(::VocabularyId).toImmutableList(),
        )

    private fun favourites(vararg ids: Long) = ids.map(::VocabularyId).toSet()

    @Test
    fun `a preset with nothing favourited reads as empty`() {
        assertEquals(PresetFavouriteState.NONE, favouriteStateOf(preset(1, 2, 3), favourites()))
    }

    @Test
    fun `a preset with every word favourited reads as full`() {
        assertEquals(PresetFavouriteState.ALL, favouriteStateOf(preset(1, 2, 3), favourites(1, 2, 3)))
    }

    /** The state a boolean cannot express, and the one where a tap changes the most. */
    @Test
    fun `a partly favourited preset reads as partial, not as full or empty`() {
        assertEquals(PresetFavouriteState.SOME, favouriteStateOf(preset(1, 2, 3), favourites(2)))
    }

    @Test
    fun `favourites outside the preset do not make it look favourited`() {
        assertEquals(PresetFavouriteState.NONE, favouriteStateOf(preset(1, 2), favourites(8, 9)))
    }

    @Test
    fun `a preset counts as full even when other words are also favourited`() {
        assertEquals(PresetFavouriteState.ALL, favouriteStateOf(preset(1, 2), favourites(1, 2, 3, 4)))
    }

    private fun word(text: String) = PresetWord(VocabularyId(text.hashCode().toLong()), text, "", "")

    @Test
    fun `words sort alphabetically`() {
        val sorted = listOf(word("zupa"), word("chleb"), word("mleko")).sortedForDisplay()

        assertEquals(listOf("chleb", "mleko", "zupa"), sorted.map { it.text })
    }

    /**
     * Polish collation, not code-point order: sorting raw strings puts every accented word
     * after z, so ćma and łódź would end up at the bottom of the list instead of in place.
     */
    @Test
    fun `accented letters sort next to their base letter, not after z`() {
        val sorted = listOf(word("zupa"), word("ćma"), word("cebula"), word("łódź"), word("lampa"))
            .sortedForDisplay()

        assertEquals(listOf("cebula", "ćma", "lampa", "łódź", "zupa"), sorted.map { it.text })
    }

    @Test
    fun `sorting ignores case`() {
        val sorted = listOf(word("Warszawa"), word("adres"), word("Kraków")).sortedForDisplay()

        assertEquals(listOf("adres", "Kraków", "Warszawa"), sorted.map { it.text })
    }
}
