package com.lexicon.common

import java.time.LocalDate

internal actual fun nowEpochMillis(): Long = System.currentTimeMillis()

internal actual fun todayEpochDay(): Long = LocalDate.now().toEpochDay()
