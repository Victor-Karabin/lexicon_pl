package com.lexicon.application.training

import kotlin.random.Random

internal fun <T> List<T>.nextToPractise(
    wasAsked: (T) -> Boolean,
    isMastered: (T) -> Boolean,
    attempts: (T) -> Int,
    random: Random,
): T? {
    if (isEmpty()) return null
    val fresh = filterNot(wasAsked).ifEmpty { this }
    val pool = fresh.filterNot(isMastered).ifEmpty { fresh }
    val fewest = pool.minOf(attempts)
    return pool.filter { attempts(it) == fewest }.random(random)
}
