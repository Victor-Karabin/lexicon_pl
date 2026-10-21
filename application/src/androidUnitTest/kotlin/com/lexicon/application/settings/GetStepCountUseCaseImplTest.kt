package com.lexicon.application.settings

import com.lexicon.boundary.AppSettingsBoundary
import com.lexicon.boundary.SettingsRepository
import com.lexicon.boundary.ThemeModeBoundary
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetStepCountUseCaseImplTest {
    private val settingsRepository: SettingsRepository = mockk()

    @Test
    fun `the step count is the learner's setting`() =
        runTest {
            coEvery { settingsRepository.getSettings() } returns AppSettingsBoundary(ThemeModeBoundary.SYSTEM, stepCount = 7)

            assertEquals(7, GetStepCountUseCaseImpl(StepCountResolver(settingsRepository))())
        }
}
