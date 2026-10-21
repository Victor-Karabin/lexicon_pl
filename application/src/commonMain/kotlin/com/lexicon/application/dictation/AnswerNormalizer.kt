package com.lexicon.application.dictation

import com.lexicon.interactors.course.LessonAnswerChecker

class AnswerNormalizer {
    fun matches(
        expected: String,
        submitted: String,
    ): Boolean = normalize(expected) == normalize(submitted)

    fun matchesIgnoringPunctuation(
        expected: String,
        submitted: String,
    ): Boolean = LessonAnswerChecker.same(expected, submitted)

    fun matchesSpoken(
        expected: String,
        submitted: String,
    ): Boolean = matchesIgnoringPunctuation(expected, submitted)

    private fun normalize(text: String): String = text.trim().lowercase()
}
