package com.lexicon.model.program

/**
 * The states a program's configuration is written in. They live here rather than with
 * the stored configuration because the rules switch on them: what a goal measures, where
 * a program's words come from, what a day's activity asks for.
 */
enum class TargetType {
    VOCABULARY,
    LESSONS,
    TOPICS,
    ACCURACY,
    RETENTION,
    STUDY_TIME,
    STREAK,
    EXERCISES,
}

enum class ScopeSourceType { PRESET, STUDY_SET, CEFR_LEVEL, LESSON, ALL }

enum class LearningStrategy { NEW_FIRST, REVIEWS_FIRST, MIXED, TOPIC_BY_TOPIC, ADAPTIVE }

enum class ActivityType { LEARN, REVIEW, PRONOUNCE, LISTEN, WRITE, MIXED, CHALLENGE }

enum class AdaptationTrigger {
    HIGH_ACCURACY,
    LOW_ACCURACY,
    MISSED_DAY,
    MISSED_WEEK,
    FAST_PROGRESS,
    SLOW_PROGRESS,
    REVIEW_BACKLOG,
}

enum class AdaptationAction {
    INCREASE_NEW_WORDS,
    DECREASE_NEW_WORDS,
    INCREASE_REVIEWS,
    PAUSE_NEW_WORDS,
    EXTEND_DURATION,
    REPEAT_MILESTONE,
}

enum class ScopeOrdering {
    AS_LISTED,
    FREQUENCY,
    DIFFICULTY,
    ALPHABETICAL,
    RANDOM,
    ;

    /**
     * Puts a program's resolved word ids in the order it asks for.
     *
     * FREQUENCY and ALPHABETICAL currently order nothing, and DIFFICULTY orders by id.
     * That is what the code has always done; whether those three were meant to sort and
     * never did, or are placeholders, cannot be read from the project, so the behaviour
     * is kept as found rather than guessed at. Gathering it here at least makes it one
     * visible decision instead of a `when` buried in a use case.
     */
    fun applyTo(wordIds: List<Long>): List<Long> =
        when (this) {
            AS_LISTED, FREQUENCY, ALPHABETICAL -> wordIds
            DIFFICULTY -> wordIds.sorted()
            RANDOM -> wordIds.shuffled()
        }
}
