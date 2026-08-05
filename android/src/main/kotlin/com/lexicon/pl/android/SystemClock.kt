package com.lexicon.pl.android

import com.lexicon.pl.common.Clock
import javax.inject.Inject

class SystemClock
    @Inject
    constructor() : Clock {
        override fun nowEpochMillis(): Long = System.currentTimeMillis()
    }
