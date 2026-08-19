package com.lexicon.model.program

import com.lexicon.model.vocabulary.Word

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

    fun applyTo(words: List<Word>): List<Word> =
        when (this) {
            AS_LISTED -> words
            FREQUENCY -> words.sortedBy { it.id.value }
            DIFFICULTY -> words.sortedWith(compareBy({ it.cefr?.ordinal ?: Int.MAX_VALUE }, { it.id.value }))
            ALPHABETICAL -> words.sortedBy { it.text.lowercase() }
            RANDOM -> words.shuffled()
        }
}
