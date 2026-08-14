package com.lexicon.common

/** Wall-clock milliseconds, however the platform reports them. */
internal expect fun nowEpochMillis(): Long

class SystemClock : Clock {
    override fun nowEpochMillis(): Long = com.lexicon.common.nowEpochMillis()
}
