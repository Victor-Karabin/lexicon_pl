package com.lexicon.common

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * How well a word came back, which is what decides when it should come back again.
 *
 * The trainings report an outcome and whether a tip was taken; that is less
 * information than SM-2's usual nought-to-five grade, so [quality] maps what is
 * available onto the part of the scale that matters — above or below the line where
 * a word is considered recalled.
 */
enum class RecallQuality(val quality: Int) {
    /** Answered correctly, unaided. */
    PERFECT(5),

    /** Correct, but a tip was needed, so the memory is not yet standing on its own. */
    HESITANT(3),

    /** Passed over. Not knowing and not saying are the same thing for scheduling. */
    SKIPPED(2),

    /** Answered wrongly. */
    FORGOTTEN(1),
    ;

    val isRecalled: Boolean get() = quality >= RECALLED_THRESHOLD
}

/** SM-2's dividing line: below this the word is treated as forgotten and relearned. */
private const val RECALLED_THRESHOLD = 3

/**
 * The knobs a program's review strategy can turn.
 *
 * Defaults are the classic SM-2 ones, with a mastery threshold of three weeks —
 * long enough that a word reaching it has survived several spaced recalls.
 */
data class ReviewSettings(
    val firstIntervalDays: Long = 1,
    val secondIntervalDays: Long = 6,
    val minIntervalDays: Long = 1,
    val maxIntervalDays: Long = 365,
    val startingEase: Double = 2.5,
    val minimumEase: Double = 1.3,
    val masteredIntervalDays: Long = 21,
)

/**
 * What is known about one word's memory: how many times it has come back
 * successfully in a row, how quickly its interval grows, and when it is next due.
 */
data class ReviewState(
    val repetitions: Int = 0,
    val easeFactor: Double = 2.5,
    val intervalDays: Long = 0,
    val dueAtEpochDay: Long = 0,
    val lapses: Int = 0,
) {
    /** A word that has been answered at least once, so it is no longer brand new. */
    val isLearned: Boolean get() = repetitions > 0 || lapses > 0

    fun isMastered(settings: ReviewSettings): Boolean = intervalDays >= settings.masteredIntervalDays
}

/**
 * The word's memory after this answer, by SM-2.
 *
 * A recalled word's interval grows by its ease; a forgotten one goes back to the
 * start of the ladder rather than all the way to unseen, because the word is still
 * familiar and only the spacing was wrong. Ease drifts down on every stumble and up
 * only on effortless recall, and never below [ReviewSettings.minimumEase] — an
 * unbounded floor is what makes a hard word come back every day forever.
 */
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

/** SM-2's ease adjustment: flat at a perfect answer, increasingly punishing below it. */
private fun easeDelta(quality: Int): Double {
    val miss = (MAX_QUALITY - quality).toDouble()
    return EASE_GAIN - miss * (EASE_LOSS_LINEAR + miss * EASE_LOSS_QUADRATIC)
}

private const val MAX_QUALITY = 5
private const val EASE_GAIN = 0.1
private const val EASE_LOSS_LINEAR = 0.08
private const val EASE_LOSS_QUADRATIC = 0.02

/** How many of [dueAtEpochDay] have passed, negative when the word is not due yet. */
fun ReviewState.daysOverdue(todayEpochDay: Long): Long = todayEpochDay - dueAtEpochDay

/** Words waiting, most overdue first, so a backlog is worked off oldest-first. */
fun <T> Map<T, ReviewState>.dueOn(
    todayEpochDay: Long,
    limit: Int = Int.MAX_VALUE,
): List<T> =
    entries
        .filter { it.value.dueAtEpochDay <= todayEpochDay }
        .sortedByDescending { it.value.daysOverdue(todayEpochDay) }
        .take(min(limit, size))
        .map { it.key }
