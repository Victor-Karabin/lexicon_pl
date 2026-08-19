package com.lexicon.presentation.presets

import com.lexicon.interactors.presets.GetWordPresetMembershipsUseCase
import com.lexicon.interactors.presets.PresetCategory
import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.PresetMembership
import com.lexicon.interactors.presets.SetWordPresetMembershipUseCase
import com.lexicon.interactors.presets.VocabularyPreset
import com.lexicon.model.vocabulary.LocalizedText
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class ChangePresetsControllerTest {
    private val dispatcher = StandardTestDispatcher()

    private val kot = Word(VocabularyId(1L), "kot", "cat", "kɔt")

    private val getMemberships: GetWordPresetMembershipsUseCase = mockk()
    private val setMembership: SetWordPresetMembershipUseCase = mockk(relaxed = true)

    private fun preset(id: String) =
        VocabularyPreset(
            id = PresetId(id),
            title = LocalizedText(mapOf("en" to id)),
            description = LocalizedText(mapOf("en" to id)),
            category = PresetCategory(id = "cat", order = 0, title = LocalizedText(mapOf("en" to "Category"))),
            icon = null,
            color = null,
            popularity = 0,
            estimatedDuration = 0.seconds,
            vocabularyIds = persistentListOf(),
        )

    private fun memberships(vararg pairs: Pair<String, Boolean>) =
        pairs.map { (id, isMember) -> PresetMembership(preset(id), isMember) }.toImmutableList()

    private fun TestScope.controller() = ChangePresetsController(this, dispatcher, getMemberships, setMembership)

    @Test
    fun `opening loads the word's memberships and counts the ones it is in`() =
        runTest(dispatcher) {
            coEvery { getMemberships(kot.id) } returns memberships("food" to true, "travel" to false, "verbs" to true)

            val controller = controller()
            controller.open(kot, languageTag = "en")
            dispatcher.scheduler.advanceUntilIdle()

            val state = requireNotNull(controller.state.value)
            assertFalse(state.isLoading)
            assertEquals("kot", state.word)
            assertEquals(2, state.memberCount)
        }

    @Test
    fun `toggling a preset on updates the chip and persists the change`() =
        runTest(dispatcher) {
            coEvery { getMemberships(kot.id) } returns memberships("food" to false)

            val controller = controller()
            controller.open(kot, languageTag = "en")
            dispatcher.scheduler.advanceUntilIdle()

            controller.toggle(PresetId("food"), isMember = true)
            dispatcher.scheduler.advanceUntilIdle()

            assertTrue(requireNotNull(controller.state.value).memberships.single().isMember)
            coVerify { setMembership(PresetId("food"), kot.id, true) }
        }

    @Test
    fun `toggling a preset off leaves the word in the others — membership is not exclusive`() =
        runTest(dispatcher) {
            coEvery { getMemberships(kot.id) } returns memberships("food" to true, "verbs" to true)

            val controller = controller()
            controller.open(kot, languageTag = "en")
            dispatcher.scheduler.advanceUntilIdle()

            controller.toggle(PresetId("food"), isMember = false)
            dispatcher.scheduler.advanceUntilIdle()

            val state = requireNotNull(controller.state.value)
            assertEquals(1, state.memberCount)
            assertTrue(state.memberships.first { it.preset.id == PresetId("verbs") }.isMember)
            coVerify { setMembership(PresetId("food"), kot.id, false) }
        }

    @Test
    fun `dismissing closes the sheet`() =
        runTest(dispatcher) {
            coEvery { getMemberships(kot.id) } returns memberships("food" to true)

            val controller = controller()
            controller.open(kot, languageTag = "en")
            dispatcher.scheduler.advanceUntilIdle()
            controller.dismiss()

            assertNull(controller.state.value)
        }

    @Test
    fun `a toggle after dismissal is ignored rather than written against no word`() =
        runTest(dispatcher) {
            val controller = controller()
            controller.toggle(PresetId("food"), isMember = true)
            dispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) { setMembership(any(), any(), any()) }
        }
}
