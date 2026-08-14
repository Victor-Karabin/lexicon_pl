package com.lexicon.common

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

private const val MILLIS_PER_SECOND = 1000

internal actual fun nowEpochMillis(): Long = (NSDate().timeIntervalSince1970 * MILLIS_PER_SECOND).toLong()
