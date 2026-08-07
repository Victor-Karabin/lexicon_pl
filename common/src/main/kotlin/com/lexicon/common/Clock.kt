package com.lexicon.common

/** Injectable time source so business logic and tests never call [System.currentTimeMillis] directly. */
interface Clock {
    fun nowEpochMillis(): Long
}
