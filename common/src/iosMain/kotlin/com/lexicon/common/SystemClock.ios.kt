package com.lexicon.common

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.timeZoneForSecondsFromGMT

private const val MILLIS_PER_SECOND = 1000
private const val SECONDS_PER_DAY = 86_400.0

private val utcCalendar: NSCalendar =
    NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian).apply {
        timeZone = NSTimeZone.timeZoneForSecondsFromGMT(0)
    }

internal actual fun nowEpochMillis(): Long = (NSDate().timeIntervalSince1970 * MILLIS_PER_SECOND).toLong()

internal actual fun todayEpochDay(): Long {
    val today = NSCalendar.currentCalendar.components(
        NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay,
        fromDate = NSDate(),
    )
    val midnightInUtc = checkNotNull(utcCalendar.dateFromComponents(today)) { "today's date has no Gregorian midnight" }
    return (midnightInUtc.timeIntervalSince1970 / SECONDS_PER_DAY).toLong()
}
