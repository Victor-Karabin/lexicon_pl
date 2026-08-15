package com.lexicon.common

/** Wall-clock milliseconds, however the platform reports them. */
internal expect fun nowEpochMillis(): Long

/** Today in the device's own time zone, as days since the epoch. */
internal expect fun todayEpochDay(): Long

class SystemClock : Clock {
    override fun nowEpochMillis(): Long = com.lexicon.common.nowEpochMillis()

    override fun todayEpochDay(): Long = com.lexicon.common.todayEpochDay()
}
