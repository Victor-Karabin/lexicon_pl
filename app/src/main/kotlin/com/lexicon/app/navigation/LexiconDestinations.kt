package com.lexicon.app.navigation

import com.lexicon.presentation.common.TRAINING_WORDS_ARG
import com.lexicon.presentation.common.asTrainingWordsArgument
import com.lexicon.presentation.course.COURSE_ID_ARG
import com.lexicon.presentation.main.MainTab
import com.lexicon.presentation.main.TrainingIds
import com.lexicon.presentation.presets.WORD_ID_ARG
import com.lexicon.presentation.program.PROGRAM_ID_ARG

internal object LexiconDestinations {
    const val SPLASH = "splash"
    const val MAIN_TAB_ARG = "tab"

    const val MAIN = "main?$MAIN_TAB_ARG={$MAIN_TAB_ARG}"

    fun main(tab: MainTab? = null) = "main?$MAIN_TAB_ARG=${tab?.name.orEmpty()}"

    const val DICTATION = TrainingIds.DICTATION
    const val DICTATION_PUZZLE = TrainingIds.DICTATION_PUZZLE
    const val TRUE_OR_FALSE = TrainingIds.TRUE_OR_FALSE
    const val WORD_MATCH = TrainingIds.WORD_MATCH
    const val PRONUNCIATION_CHECK = TrainingIds.PRONUNCIATION_CHECK
    const val PUZZLE = TrainingIds.PUZZLE
    const val IMAGE_TEST = TrainingIds.IMAGE_TEST
    const val MEMORY_CARDS = TrainingIds.MEMORY_CARDS
    const val MIX = TrainingIds.MIX
    const val WORD_CARD = TrainingIds.WORD_CARD
    const val CROSSWORD = TrainingIds.CROSSWORD

    fun trainingRoute(training: String) = "$training?$TRAINING_WORDS_ARG={$TRAINING_WORDS_ARG}&$PROGRAM_RUN_ARG={$PROGRAM_RUN_ARG}"

    fun scopedTraining(
        training: String,
        wordIds: List<Long>,
        programId: String? = null,
    ) = "$training?$TRAINING_WORDS_ARG=${wordIds.asTrainingWordsArgument()}" +
        "&$PROGRAM_RUN_ARG=${programId.orEmpty()}"

    const val PRESET_DETAIL = "preset/{presetId}"

    fun presetDetail(presetId: String) = "preset/$presetId"

    const val CREATE_WORD = "create/word"
    const val CREATE_PRESET = "create/preset"
    const val CREATE_PROGRAM = "create/program"
    const val EDIT_PROGRAM = "edit/program/{programId}"

    fun editProgram(id: String): String = "edit/program/$id"

    const val EDIT_WORD = "word/{$WORD_ID_ARG}/edit"

    fun editWord(wordId: Long) = "word/$wordId/edit"

    const val PROGRAM_CARDS = "program/{$PROGRAM_ID_ARG}/cards"

    fun programCards(programId: String) = "program/$programId/cards"

    const val COURSE = "course/{$COURSE_ID_ARG}"

    fun course(courseId: String) = "course/$courseId"

    const val LESSON = "lesson/{lessonId}"

    fun lesson(lessonId: String) = "lesson/$lessonId"

    const val EXERCISE = "lesson/{lessonId}/exercise/{exerciseId}"

    fun exercise(
        lessonId: String,
        exerciseId: String,
    ) = "lesson/$lessonId/exercise/$exerciseId"

    const val PROGRAM_RUN_ARG = "programRun"

    const val SESSION_RESULT =
        "session_result/{correct}/{incorrect}/{skipped}/{tipsUsed}?$PROGRAM_RUN_ARG={$PROGRAM_RUN_ARG}"

    fun sessionResult(
        correct: Int,
        incorrect: Int,
        skipped: Int,
        tipsUsed: Int,
        programId: String? = null,
    ) = "session_result/$correct/$incorrect/$skipped/$tipsUsed?$PROGRAM_RUN_ARG=${programId.orEmpty()}"

    const val DAY_COMPLETE = "program/{programId}/day_complete"

    fun dayComplete(programId: String) = "program/$programId/day_complete"
}
