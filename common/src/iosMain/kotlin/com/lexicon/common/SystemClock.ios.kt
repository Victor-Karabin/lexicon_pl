package com.lexicon.common

import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

private const val MILLIS_PER_SECOND = 1000
private const val SECONDS_PER_DAY = 86_400.0

internal actual fun nowEpochMillis(): Long = (NSDate().timeIntervalSince1970 * MILLIS_PER_SECOND).toLong()

/**
 * Midnight local, expressed as whole days since the epoch. Taking the start of the
 * day first is what makes this the local date rather than the UTC one.
 */
internal actual fun todayEpochDay(): Long {
    val startOfToday = NSCalendar.currentCalendar.startOfDayForDate(NSDate())
    return (startOfToday.timeIntervalSince1970 / SECONDS_PER_DAY).toLong()
}
