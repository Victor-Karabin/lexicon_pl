package com.lexicon.app.navigation

import com.lexicon.presentation.common.TRAINING_WORDS_ARG
import com.lexicon.presentation.common.asTrainingWordsArgument
import com.lexicon.presentation.course.COURSE_ID_ARG
import com.lexicon.presentation.main.MainTab
import com.lexicon.presentation.main.TrainingIds
import com.lexicon.presentation.presets.WORD_ID_ARG

internal object LexiconDestinations {
    const val SPLASH = "splash"
    const val MAIN_TAB_ARG = "tab"

    const val MAIN = "main?$MAIN_TAB_ARG={$MAIN_TAB_ARG}"

    fun main(tab: MainTab? = null) = "main?$MAIN_TAB_ARG=${tab?.name.orEmpty()}"

    val DICTATION = TrainingIds.DICTATION
    val DICTATION_PUZZLE = TrainingIds.DICTATION_PUZZLE
    val TRUE_OR_FALSE = TrainingIds.TRUE_OR_FALSE
    val WORD_MATCH = TrainingIds.WORD_MATCH
    val PRONUNCIATION_CHECK = TrainingIds.PRONUNCIATION_CHECK
    val PRONUNCIATION_SENTENCES = TrainingIds.PRONUNCIATION_SENTENCES
    val PUZZLE = TrainingIds.PUZZLE
    val IMAGE_TEST = TrainingIds.IMAGE_TEST
    val MEMORY_CARDS = TrainingIds.MEMORY_CARDS
    val MIX = TrainingIds.MIX
    val WORD_CARD = TrainingIds.WORD_CARD
    val CROSSWORD = TrainingIds.CROSSWORD
    val PASSAGE_WRITE = TrainingIds.PASSAGE_WRITE
    val PASSAGE_BANK = TrainingIds.PASSAGE_BANK
    val FILLWORD = TrainingIds.FILLWORD
    const val CONJUGATION = "conjugation/{courseId}"
    const val CONJUGATION_VERBS = "conjugation/verbs"
    const val REVIEW_WORDS = "review/words"

    fun conjugationCourse(courseId: String) = "conjugation/$courseId"

    fun trainingRoute(training: String) = "$training?$TRAINING_WORDS_ARG={$TRAINING_WORDS_ARG}&$COURSE_RUN_ARG={$COURSE_RUN_ARG}"

    fun scopedTraining(
        training: String,
        wordIds: List<Long>,
        inCourse: Boolean = false,
    ) = "$training?$TRAINING_WORDS_ARG=${wordIds.asTrainingWordsArgument()}" +
        "&$COURSE_RUN_ARG=${inCourse.asCourseRun()}"

    const val PRESET_DETAIL = "preset/{presetId}"

    fun presetDetail(presetId: String) = "preset/$presetId"

    const val CREATE_WORD = "create/word"
    const val CREATE_PRESET = "create/preset"
    const val COURSE_SETTINGS = "vocabulary-course/settings"
    const val COURSE_CARDS = "vocabulary-course/cards"

    const val EDIT_WORD = "word/{$WORD_ID_ARG}/edit"

    fun editWord(wordId: Long) = "word/$wordId/edit"

    const val COURSE = "course/{$COURSE_ID_ARG}"

    fun course(courseId: String) = "course/$courseId"

    const val LESSON = "lesson/{lessonId}"

    fun lesson(lessonId: String) = "lesson/$lessonId"

    const val EXERCISE = "lesson/{lessonId}/exercise/{exerciseId}"

    fun exercise(
        lessonId: String,
        exerciseId: String,
    ) = "lesson/$lessonId/exercise/$exerciseId"

    const val COURSE_RUN_ARG = "courseRun"

    const val SESSION_RESULT =
        "session_result/{correct}/{incorrect}/{skipped}/{tipsUsed}?$COURSE_RUN_ARG={$COURSE_RUN_ARG}"

    fun sessionResult(
        correct: Int,
        incorrect: Int,
        skipped: Int,
        tipsUsed: Int,
        inCourse: Boolean = false,
    ) = "session_result/$correct/$incorrect/$skipped/$tipsUsed?$COURSE_RUN_ARG=${inCourse.asCourseRun()}"

    private fun Boolean.asCourseRun(): String = if (this) COURSE_RUN else ""

    private const val COURSE_RUN = "1"
}
