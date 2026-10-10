package com.lexicon.presentation.presets

import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import org.junit.Assert.assertEquals
import org.junit.Test

class WordOrderTest {
    @Test
    fun `words follow the Polish alphabet, with each letter with a mark right after its plain letter`() {
        val words = listOf("źle", "łatwo", "zero", "ćwiczenie", "lekcja", "żona", "szkoła", "cześć", "śmiech", "okno", "ósma")
            .mapIndexed { index, text -> Word(VocabularyId(index.toLong()), text, "", "") }

        assertEquals(
            listOf("cześć", "ćwiczenie", "lekcja", "łatwo", "okno", "ósma", "szkoła", "śmiech", "zero", "źle", "żona"),
            words.sortedForDisplay().map { it.text },
        )
    }
}
