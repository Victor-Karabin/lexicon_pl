package com.lexicon.common

interface Clock {
    fun nowEpochMillis(): Long

    /**
     * Today as a count of days since the epoch, in the learner's own time zone.
     *
     * Streaks and daily plans are counted in local days, so this cannot be derived
     * from [nowEpochMillis]: an evening session east of UTC would otherwise be filed
     * under tomorrow and break a streak the learner did not break.
     */
    fun todayEpochDay(): Long
}
