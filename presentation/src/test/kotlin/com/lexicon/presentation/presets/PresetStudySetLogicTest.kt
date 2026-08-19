package com.lexicon.presentation.presets

import com.lexicon.interactors.presets.LocalizedText
import com.lexicon.interactors.presets.PresetCategory
import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.PresetStudySetState
import com.lexicon.interactors.presets.PresetWord
import com.lexicon.interactors.presets.VocabularyId
import com.lexicon.interactors.presets.VocabularyPreset
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

class PresetStudySetLogicTest {
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

    private fun studySet(vararg ids: Long) = ids.map(::VocabularyId).toSet()

    @Test
    fun `a preset with nothing studySetd reads as empty`() {
        assertEquals(PresetStudySetState.NONE, studySetStateOf(preset(1, 2, 3), studySet()))
    }

    @Test
    fun `a preset with every word studySetd reads as full`() {
        assertEquals(PresetStudySetState.ALL, studySetStateOf(preset(1, 2, 3), studySet(1, 2, 3)))
    }

    @Test
    fun `a partly studySetd preset reads as partial, not as full or empty`() {
        assertEquals(PresetStudySetState.SOME, studySetStateOf(preset(1, 2, 3), studySet(2)))
    }

    @Test
    fun `a preset reads as full once its remaining words are studySetd`() {
        val afterDeletingWordThree = preset(1, 2)

        assertEquals(PresetStudySetState.ALL, studySetStateOf(afterDeletingWordThree, studySet(1, 2)))
    }

    @Test
    fun `studySet outside the preset do not make it look studySetd`() {
        assertEquals(PresetStudySetState.NONE, studySetStateOf(preset(1, 2), studySet(8, 9)))
    }

    @Test
    fun `a preset counts as full even when other words are also studySetd`() {
        assertEquals(PresetStudySetState.ALL, studySetStateOf(preset(1, 2), studySet(1, 2, 3, 4)))
    }

    private fun word(text: String) = PresetWord(VocabularyId(text.hashCode().toLong()), text, "", "")

    @Test
    fun `words sort alphabetically`() {
        val sorted = listOf(word("zupa"), word("chleb"), word("mleko")).sortedForDisplay()

        assertEquals(listOf("chleb", "mleko", "zupa"), sorted.map { it.text })
    }

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
