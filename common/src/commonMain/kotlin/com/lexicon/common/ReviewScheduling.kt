package com.lexicon.common

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

enum class RecallQuality(val quality: Int) {
    PERFECT(5),

    HESITANT(3),

    SKIPPED(2),

    FORGOTTEN(1),
    ;

    val isRecalled: Boolean get() = quality >= RECALLED_THRESHOLD
}

private const val RECALLED_THRESHOLD = 3

data class ReviewSettings(
    val firstIntervalDays: Long = 1,
    val secondIntervalDays: Long = 6,
    val minIntervalDays: Long = 1,
    val maxIntervalDays: Long = 365,
    val startingEase: Double = 2.5,
    val minimumEase: Double = 1.3,
    val masteredIntervalDays: Long = 21,
)

data class ReviewState(
    val repetitions: Int = 0,
    val easeFactor: Double = 2.5,
    val intervalDays: Long = 0,
    val dueAtEpochDay: Long = 0,
    val lapses: Int = 0,
) {
    val isLearned: Boolean get() = repetitions > 0 || lapses > 0

    fun isMastered(settings: ReviewSettings): Boolean = intervalDays >= settings.masteredIntervalDays
}

fun ReviewState.next(
    quality: RecallQuality,
    todayEpochDay: Long,
    settings: ReviewSettings = ReviewSettings(),
): ReviewState {
    val ease = max(settings.minimumEase, easeFactor + easeDelta(quality.quality))

    if (!quality.isRecalled) {
        return copy(
            repetitions = 0,
            easeFactor = ease,
            intervalDays = settings.firstIntervalDays,
            dueAtEpochDay = todayEpochDay + settings.firstIntervalDays,
            lapses = lapses + 1,
        )
    }

    val nextInterval = when (repetitions) {
        0 -> settings.firstIntervalDays
        1 -> settings.secondIntervalDays
        else -> (intervalDays * ease).roundToLong()
    }.coerceIn(settings.minIntervalDays, settings.maxIntervalDays)

    return copy(
        repetitions = repetitions + 1,
        easeFactor = ease,
        intervalDays = nextInterval,
        dueAtEpochDay = todayEpochDay + nextInterval,
    )
}

private fun easeDelta(quality: Int): Double {
    val miss = (MAX_QUALITY - quality).toDouble()
    return EASE_GAIN - miss * (EASE_LOSS_LINEAR + miss * EASE_LOSS_QUADRATIC)
}

private const val MAX_QUALITY = 5
private const val EASE_GAIN = 0.1
private const val EASE_LOSS_LINEAR = 0.08
private const val EASE_LOSS_QUADRATIC = 0.02

fun ReviewState.daysOverdue(todayEpochDay: Long): Long = todayEpochDay - dueAtEpochDay

fun <T> Map<T, ReviewState>.dueOn(
    todayEpochDay: Long,
    limit: Int = Int.MAX_VALUE,
): List<T> =
    entries
        .filter { it.value.dueAtEpochDay <= todayEpochDay }
        .sortedByDescending { it.value.daysOverdue(todayEpochDay) }
        .take(min(limit, size))
        .map { it.key }
