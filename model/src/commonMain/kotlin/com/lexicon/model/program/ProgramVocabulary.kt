package com.lexicon.model.program

import com.lexicon.model.vocabulary.Word

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
     * Puts a program's resolved words in the order it asks for.
     *
     * Frequency is the catalogue's own id order: the shipped vocabulary is numbered by
     * how common a word is, which is why the Top 100 preset is ids 1..100. Difficulty is
     * the CEFR level, falling back to frequency inside a level and putting unlevelled
     * words last, since nothing is known about how hard they are.
     */
    fun applyTo(words: List<Word>): List<Word> =
        when (this) {
            AS_LISTED -> words
            FREQUENCY -> words.sortedBy { it.id.value }
            DIFFICULTY -> words.sortedWith(compareBy({ it.cefr?.ordinal ?: Int.MAX_VALUE }, { it.id.value }))
            ALPHABETICAL -> words.sortedBy { it.text.lowercase() }
            RANDOM -> words.shuffled()
        }
}
