package com.lexicon.interactors.settings

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode,
    /** Steps generated for a newly created training session. */
    val stepCount: Int,
) {
    companion object {
        const val DEFAULT_STEP_COUNT = 10

        /** Below 3 a session is barely a session; above 30 it stops being finishable in one sitting. */
        const val MIN_STEP_COUNT = 3
        const val MAX_STEP_COUNT = 30

        val Default = AppSettings(themeMode = ThemeMode.SYSTEM, stepCount = DEFAULT_STEP_COUNT)
    }
}
