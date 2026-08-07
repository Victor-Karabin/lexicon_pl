package com.lexicon.domain.settings

import com.lexicon.boundary.AppSettingsBoundary
import com.lexicon.boundary.SettingsRepository
import com.lexicon.boundary.ThemeModeBoundary
import com.lexicon.interactors.settings.AppSettings
import com.lexicon.interactors.settings.ThemeMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsUseCaseImplsTest {
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)

    @Test
    fun `observed settings are mapped from the boundary model`() =
        runTest {
            every { settingsRepository.observeSettings() } returns
                flowOf(AppSettingsBoundary(ThemeModeBoundary.DARK, stepCount = 15))

            val settings = ObserveSettingsUseCaseImpl(settingsRepository)().first()

            assertEquals(AppSettings(ThemeMode.DARK, stepCount = 15), settings)
        }

    @Test
    fun `theme mode is written through to the repository`() =
        runTest {
            UpdateThemeModeUseCaseImpl(settingsRepository)(ThemeMode.LIGHT)

            coVerify { settingsRepository.setThemeMode(ThemeModeBoundary.LIGHT) }
        }

    @Test
    fun `step count below the minimum is clamped up`() =
        runTest {
            UpdateStepCountUseCaseImpl(settingsRepository)(0)

            coVerify { settingsRepository.setStepCount(AppSettings.MIN_STEP_COUNT) }
        }

    @Test
    fun `step count above the maximum is clamped down`() =
        runTest {
            UpdateStepCountUseCaseImpl(settingsRepository)(999)

            coVerify { settingsRepository.setStepCount(AppSettings.MAX_STEP_COUNT) }
        }

    @Test
    fun `a step count inside the allowed range is stored unchanged`() =
        runTest {
            UpdateStepCountUseCaseImpl(settingsRepository)(12)

            coVerify { settingsRepository.setStepCount(12) }
        }

    @Test
    fun `an unset request step count falls back to the configured setting`() =
        runTest {
            coEvery { settingsRepository.getSettings() } returns
                AppSettingsBoundary(ThemeModeBoundary.SYSTEM, stepCount = 7)

            assertEquals(7, StepCountResolver(settingsRepository).resolve(requestedStepCount = null))
        }

    @Test
    fun `an explicit request step count overrides the configured setting`() =
        runTest {
            coEvery { settingsRepository.getSettings() } returns
                AppSettingsBoundary(ThemeModeBoundary.SYSTEM, stepCount = 7)

            assertEquals(3, StepCountResolver(settingsRepository).resolve(requestedStepCount = 3))
        }
}
