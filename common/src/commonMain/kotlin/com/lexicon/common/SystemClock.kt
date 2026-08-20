package com.lexicon.common

internal expect fun nowEpochMillis(): Long

internal expect fun todayEpochDay(): Long

class SystemClock : Clock {
    override fun nowEpochMillis(): Long = com.lexicon.common.nowEpochMillis()

    override fun todayEpochDay(): Long = com.lexicon.common.todayEpochDay()
}
