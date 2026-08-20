package com.lexicon.model.vocabulary

import org.junit.Assert.assertEquals
import org.junit.Test

class PresetStudySetStateTest {
    @Test
    fun `a preset nobody has starred is empty`() {
        assertEquals(PresetStudySetState.NONE, PresetStudySetState.of(wordCount = 10, studySetCount = 0))
    }

    @Test
    fun `a preset starred to the last word is full`() {
        assertEquals(PresetStudySetState.ALL, PresetStudySetState.of(wordCount = 10, studySetCount = 10))
    }

    @Test
    fun `anything in between is partly starred`() {
        assertEquals(PresetStudySetState.SOME, PresetStudySetState.of(wordCount = 10, studySetCount = 1))
        assertEquals(PresetStudySetState.SOME, PresetStudySetState.of(wordCount = 10, studySetCount = 9))
    }

    @Test
    fun `a preset with no words is empty rather than full`() {
        assertEquals(PresetStudySetState.NONE, PresetStudySetState.of(wordCount = 0, studySetCount = 0))
    }

    @Test
    fun `more starred than counted still reads as full`() {
        assertEquals(PresetStudySetState.ALL, PresetStudySetState.of(wordCount = 3, studySetCount = 4))
    }
}
