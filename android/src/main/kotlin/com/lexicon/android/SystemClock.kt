package com.lexicon.android

import com.lexicon.common.Clock
import javax.inject.Inject

class SystemClock
    @Inject
    constructor() : Clock {
        override fun nowEpochMillis(): Long = System.currentTimeMillis()
    }
