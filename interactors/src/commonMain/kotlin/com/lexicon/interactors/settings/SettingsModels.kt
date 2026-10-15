package com.lexicon.interactors.settings

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode,
    val stepCount: Int,
    val voiceId: String? = null,
    val passMark: Int = DEFAULT_PASS_MARK,
) {
    companion object {
        const val DEFAULT_STEP_COUNT = 10

        const val MIN_STEP_COUNT = 3
        const val MAX_STEP_COUNT = 30

        const val DEFAULT_PASS_MARK = 80

        const val PASS_MARK_STEP = 10

        val PassMarks = 0..100

        val Default = AppSettings(themeMode = ThemeMode.SYSTEM, stepCount = DEFAULT_STEP_COUNT, voiceId = null)
    }
}
