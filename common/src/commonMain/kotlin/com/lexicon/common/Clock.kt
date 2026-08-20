package com.lexicon.common

interface Clock {
    fun nowEpochMillis(): Long

    fun todayEpochDay(): Long
}
