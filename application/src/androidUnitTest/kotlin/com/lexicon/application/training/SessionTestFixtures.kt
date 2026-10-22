package com.lexicon.application.training

import com.lexicon.application.settings.StepCountResolver
import com.lexicon.boundary.AppSettingsBoundary
import com.lexicon.boundary.SettingsRepository
import com.lexicon.boundary.ThemeModeBoundary
import com.lexicon.model.training.Session
import com.lexicon.model.training.SessionId
import com.lexicon.model.training.Step
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import io.mockk.coEvery
import io.mockk.mockk

internal const val SETTINGS_STEP_COUNT = 10

internal fun stepCountResolver(stepCount: Int = SETTINGS_STEP_COUNT): StepCountResolver {
    val settings: SettingsRepository = mockk {
        coEvery { getSettings() } returns AppSettingsBoundary(ThemeModeBoundary.SYSTEM, stepCount = stepCount)
    }
    return StepCountResolver(settings)
}

internal val sessionWords = listOf(
    Word(VocabularyId(1), "kot", "cat", "kɔt"),
    Word(VocabularyId(2), "pies", "dog", "pjɛs"),
)

internal suspend fun FakeSessionStore.sessionOf(sessionId: String): Session? = find(SessionId(sessionId))

internal fun Session.questions(): List<Step.Question> = steps.map { it as Step.Question }
