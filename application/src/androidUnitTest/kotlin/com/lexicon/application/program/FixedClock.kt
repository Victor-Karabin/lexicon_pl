package com.lexicon.application.program

import com.lexicon.common.Clock

private const val MILLIS_A_DAY = 86_400_000L

class FixedClock(
    private val nowMillis: Long,
) : Clock {
    override fun nowEpochMillis(): Long = nowMillis

    override fun todayEpochDay(): Long = nowMillis / MILLIS_A_DAY
}
