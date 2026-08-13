package com.lexicon.android

import com.lexicon.common.Clock

class SystemClock : Clock {
    override fun nowEpochMillis(): Long = System.currentTimeMillis()
}
