package com.lexicon.data.local

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Re-seeding the preset catalogue clears preset_words wholesale, so the learner's own
 * membership edits only survive because they are replayed from preset_word_overrides
 * afterwards. That replay is what these cover — without it an app update would
 * silently undo every change the learner made.
 */
class PresetWordOverrideTest {
    private val dao: PresetDao = mockk(relaxed = true)

    private fun preset(id: String) =
        PresetEntity(
            id = id,
            categoryId = "cat",
            titleJson = "{}",
            descriptionJson = "{}",
            icon = null,
            color = null,
            popularity = 0,
            estimatedSeconds = 0,
        )

    @Test
    fun `a word the learner added is put back after the catalogue is re-seeded`() =
        runTest {
            coEvery { dao.getOverrides() } returns listOf(PresetWordOverrideEntity("food", 1L, isMember = true))
            coEvery { dao.nextPosition("food") } returns 7
            coEvery { dao.replaceCatalog(any(), any(), any()) } answers { callOriginal() }

            dao.replaceCatalog(categories = emptyList(), presets = listOf(preset("food")), memberships = emptyList())

            val inserted = slot<PresetWordEntity>()
            coVerify { dao.insertMembership(capture(inserted)) }
            assertEquals(PresetWordEntity(presetId = "food", wordId = 1L, position = 7), inserted.captured)
        }

    @Test
    fun `a word the learner removed is taken out again after the catalogue is re-seeded`() =
        runTest {
            coEvery { dao.getOverrides() } returns listOf(PresetWordOverrideEntity("food", 1L, isMember = false))
            coEvery { dao.replaceCatalog(any(), any(), any()) } answers { callOriginal() }

            dao.replaceCatalog(
                categories = emptyList(),
                presets = listOf(preset("food")),
                memberships = listOf(PresetWordEntity("food", 1L, position = 0)),
            )

            coVerify { dao.deleteMembership("food", 1L) }
        }

    @Test
    fun `an override for a preset the catalogue dropped is skipped, not resurrected`() =
        runTest {
            coEvery { dao.getOverrides() } returns listOf(PresetWordOverrideEntity("retired", 1L, isMember = true))
            coEvery { dao.replaceCatalog(any(), any(), any()) } answers { callOriginal() }

            dao.replaceCatalog(categories = emptyList(), presets = listOf(preset("food")), memberships = emptyList())

            coVerify(exactly = 0) { dao.insertMembership(any()) }
        }
}
