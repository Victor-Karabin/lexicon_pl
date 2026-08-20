package com.lexicon.model.scheduling

private const val MILLIS_PER_SECOND = 1000L

data class StudyTimePolicy(
    val longestCreditedGapSeconds: Long = 120,
) {
    fun creditedSeconds(
        answeredAtEpochMillis: Long,
        previousAnswerAtEpochMillis: Long?,
    ): Long {
        val previous = previousAnswerAtEpochMillis ?: return 0
        val gapSeconds = (answeredAtEpochMillis - previous) / MILLIS_PER_SECOND
        return gapSeconds.coerceIn(0, longestCreditedGapSeconds)
    }
}
