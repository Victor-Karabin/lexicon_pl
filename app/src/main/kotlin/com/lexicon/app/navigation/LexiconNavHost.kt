package com.lexicon.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lexicon.presentation.common.SessionResultScreen
import com.lexicon.presentation.common.TRAINING_WORDS_ARG
import com.lexicon.presentation.common.TrainingGate
import com.lexicon.presentation.common.TrainingRequirements
import com.lexicon.presentation.course.COURSE_ID_ARG
import com.lexicon.presentation.course.CourseDetailScreen
import com.lexicon.presentation.course.EXERCISE_ID_ARG
import com.lexicon.presentation.course.ExerciseScreen
import com.lexicon.presentation.course.LESSON_ID_ARG
import com.lexicon.presentation.course.LessonScreen
import com.lexicon.presentation.crossword.CrosswordScreen
import com.lexicon.presentation.dictation.DictationScreen
import com.lexicon.presentation.dictationpuzzle.DictationPuzzleScreen
import com.lexicon.presentation.imagetest.ImageTestScreen
import com.lexicon.presentation.main.MainScreen
import com.lexicon.presentation.main.SplashScreen
import com.lexicon.presentation.main.trainingDisplayName
import com.lexicon.presentation.memorycards.MemoryCardsScreen
import com.lexicon.presentation.mix.MixScreen
import com.lexicon.presentation.presets.CreatePresetScreen
import com.lexicon.presentation.presets.CreateWordScreen
import com.lexicon.presentation.presets.PRESET_ID_ARG
import com.lexicon.presentation.presets.PresetDetailScreen
import com.lexicon.presentation.presets.WORD_ID_ARG
import com.lexicon.presentation.program.PROGRAM_ID_ARG
import com.lexicon.presentation.program.ProgramScreen
import com.lexicon.presentation.pronunciation.PronunciationScreen
import com.lexicon.presentation.puzzle.PuzzleScreen
import com.lexicon.presentation.trueorfalse.TrueOrFalseScreen
import com.lexicon.presentation.wordmatch.WordMatchScreen

@Composable
fun LexiconNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = LexiconDestinations.SPLASH) {
        composable(LexiconDestinations.SPLASH) {
            SplashScreen(
                onFinished = {
                    navController.navigate(LexiconDestinations.MAIN) {
                        popUpTo(LexiconDestinations.SPLASH) { inclusive = true }
                    }
                },
            )
        }

        composable(LexiconDestinations.MAIN) {
            MainScreen(
                onTrainingSelected = { route -> navController.navigate(route) },
                onPresetSelected = { id -> navController.navigate(LexiconDestinations.presetDetail(id)) },
                onCourseSelected = { id -> navController.navigate(LexiconDestinations.course(id)) },
                onProgramSelected = { id -> navController.navigate(LexiconDestinations.program(id)) },
                onEditWord = { id -> navController.navigate(LexiconDestinations.editWord(id)) },
                onAddWord = { navController.navigate(LexiconDestinations.CREATE_WORD) },
                onAddPreset = { navController.navigate(LexiconDestinations.CREATE_PRESET) },
            )
        }

        composable(LexiconDestinations.CREATE_WORD) {
            CreateWordScreen(
                onClose = { navController.popBackStack() },
                // Straight back to the list, which re-reads on resume and so already
                // shows the new word.
                onCreated = { navController.popBackStack() },
            )
        }

        composable(
            route = LexiconDestinations.EDIT_WORD,
            arguments = listOf(navArgument(WORD_ID_ARG) { type = NavType.StringType }),
        ) {
            CreateWordScreen(
                onClose = { navController.popBackStack() },
                onCreated = { navController.popBackStack() },
            )
        }

        composable(LexiconDestinations.CREATE_PRESET) {
            CreatePresetScreen(
                onClose = { navController.popBackStack() },
                onCreated = { navController.popBackStack() },
            )
        }

        composable(
            route = LexiconDestinations.PRESET_DETAIL,
            arguments = listOf(navArgument(PRESET_ID_ARG) { type = NavType.StringType }),
        ) {
            PresetDetailScreen(
                onClose = { navController.popBackStack() },
                onEditWord = { id -> navController.navigate(LexiconDestinations.editWord(id.value)) },
            )
        }

        composable(
            route = LexiconDestinations.PROGRAM,
            arguments = listOf(navArgument(PROGRAM_ID_ARG) { type = NavType.StringType }),
        ) {
            ProgramScreen(onClose = { navController.popBackStack() })
        }

        composable(
            route = LexiconDestinations.COURSE,
            arguments = listOf(navArgument(COURSE_ID_ARG) { type = NavType.StringType }),
        ) {
            CourseDetailScreen(
                onClose = { navController.popBackStack() },
                onLessonSelected = { id -> navController.navigate(LexiconDestinations.lesson(id.value)) },
            )
        }

        composable(
            route = LexiconDestinations.LESSON,
            arguments = listOf(navArgument(LESSON_ID_ARG) { type = NavType.StringType }),
        ) {
            val lessonId = it.arguments?.getString(LESSON_ID_ARG).orEmpty()
            LessonScreen(
                onClose = { navController.popBackStack() },
                onEditWord = { id -> navController.navigate(LexiconDestinations.editWord(id.value)) },
                onExerciseSelected = { exercise ->
                    navController.navigate(LexiconDestinations.exercise(lessonId, exercise.id))
                },
                onTrainLesson = { wordIds ->
                    navController.navigate(LexiconDestinations.scopedTraining(LexiconDestinations.MIX, wordIds))
                },
            )
        }

        composable(
            route = LexiconDestinations.EXERCISE,
            arguments = listOf(
                navArgument(LESSON_ID_ARG) { type = NavType.StringType },
                navArgument(EXERCISE_ID_ARG) { type = NavType.StringType },
            ),
        ) {
            ExerciseScreen(onClose = { navController.popBackStack() })
        }

        fun onStepSessionComplete(training: String): (Int, Int, Int, Int) -> Unit =
            { correct, incorrect, skipped, tipsUsed ->
                navController.navigate(LexiconDestinations.sessionResult(correct, incorrect, skipped, tipsUsed)) {
                    popUpTo(LexiconDestinations.trainingRoute(training)) { inclusive = true }
                }
            }

        val closeToMain: () -> Unit = { navController.popBackStack(LexiconDestinations.MAIN, inclusive = false) }

        trainingDestination(
            training = LexiconDestinations.DICTATION,
            minimumWords = TrainingRequirements.SINGLE_WORD_STEP,
            onClose = closeToMain,
            onComplete = onStepSessionComplete(LexiconDestinations.DICTATION),
        ) { onComplete ->
            DictationScreen(onSessionComplete = onComplete, onClose = closeToMain)
        }
        trainingDestination(
            training = LexiconDestinations.DICTATION_PUZZLE,
            minimumWords = TrainingRequirements.SINGLE_WORD_STEP,
            onClose = closeToMain,
            onComplete = onStepSessionComplete(LexiconDestinations.DICTATION_PUZZLE),
        ) { onComplete ->
            DictationPuzzleScreen(onSessionComplete = onComplete, onClose = closeToMain)
        }
        trainingDestination(
            training = LexiconDestinations.TRUE_OR_FALSE,
            minimumWords = TrainingRequirements.TRUE_OR_FALSE,
            onClose = closeToMain,
            onComplete = onStepSessionComplete(LexiconDestinations.TRUE_OR_FALSE),
        ) { onComplete ->
            TrueOrFalseScreen(onSessionComplete = onComplete, onClose = closeToMain)
        }
        trainingDestination(
            training = LexiconDestinations.WORD_MATCH,
            minimumWords = TrainingRequirements.WORD_MATCH,
            onClose = closeToMain,
            onComplete = onStepSessionComplete(LexiconDestinations.WORD_MATCH),
        ) { onComplete ->
            WordMatchScreen(onSessionComplete = onComplete, onClose = closeToMain)
        }
        trainingDestination(
            training = LexiconDestinations.PRONUNCIATION_CHECK,
            minimumWords = TrainingRequirements.SINGLE_WORD_STEP,
            onClose = closeToMain,
            onComplete = onStepSessionComplete(LexiconDestinations.PRONUNCIATION_CHECK),
        ) { onComplete ->
            PronunciationScreen(onSessionComplete = onComplete, onClose = closeToMain)
        }
        trainingDestination(
            training = LexiconDestinations.PUZZLE,
            minimumWords = TrainingRequirements.SINGLE_WORD_STEP,
            onClose = closeToMain,
            onComplete = onStepSessionComplete(LexiconDestinations.PUZZLE),
        ) { onComplete ->
            PuzzleScreen(onSessionComplete = onComplete, onClose = closeToMain)
        }
        trainingDestination(
            training = LexiconDestinations.IMAGE_TEST,
            minimumWords = TrainingRequirements.IMAGE_TEST,
            onClose = closeToMain,
            onComplete = onStepSessionComplete(LexiconDestinations.IMAGE_TEST),
        ) { onComplete ->
            ImageTestScreen(onSessionComplete = onComplete, onClose = closeToMain)
        }
        trainingDestination(
            training = LexiconDestinations.MEMORY_CARDS,
            minimumWords = TrainingRequirements.MEMORY_CARDS,
            onClose = closeToMain,
            onComplete = onStepSessionComplete(LexiconDestinations.MEMORY_CARDS),
        ) { onComplete ->
            MemoryCardsScreen(onSessionComplete = onComplete, onClose = closeToMain)
        }
        trainingDestination(
            training = LexiconDestinations.CROSSWORD,
            minimumWords = TrainingRequirements.CROSSWORD,
            onClose = closeToMain,
            onComplete = onStepSessionComplete(LexiconDestinations.CROSSWORD),
        ) { onComplete ->
            CrosswordScreen(onSessionComplete = onComplete, onClose = closeToMain)
        }
        trainingDestination(
            training = LexiconDestinations.MIX,
            minimumWords = TrainingRequirements.MIX,
            onClose = closeToMain,
            onComplete = onStepSessionComplete(LexiconDestinations.MIX),
        ) { onComplete ->
            MixScreen(onSessionComplete = onComplete, onClose = closeToMain)
        }

        composable(
            route = LexiconDestinations.SESSION_RESULT,
            arguments =
                listOf(
                    navArgument("correct") { type = NavType.IntType },
                    navArgument("incorrect") { type = NavType.IntType },
                    navArgument("skipped") { type = NavType.IntType },
                    navArgument("tipsUsed") { type = NavType.IntType },
                ),
        ) { backStackEntry ->
            val args = backStackEntry.arguments
            SessionResultScreen(
                correct = args?.getInt("correct").orDefault(),
                incorrect = args?.getInt("incorrect").orDefault(),
                skipped = args?.getInt("skipped").orDefault(),
                tipsUsed = args?.getInt("tipsUsed").orDefault(),
                onDone = { navController.popBackStack(LexiconDestinations.MAIN, inclusive = false) },
            )
        }
    }
}

/**
 * A training destination: the shared not-enough-words gate, plus the optional word
 * list a course lesson uses to narrow the session.
 */
private fun NavGraphBuilder.trainingDestination(
    training: String,
    minimumWords: Int,
    onClose: () -> Unit,
    onComplete: (Int, Int, Int, Int) -> Unit,
    screen: @Composable (onComplete: (Int, Int, Int, Int) -> Unit) -> Unit,
) {
    composable(
        route = LexiconDestinations.trainingRoute(training),
        arguments = listOf(
            navArgument(TRAINING_WORDS_ARG) {
                type = NavType.StringType
                defaultValue = ""
            },
        ),
    ) { backStackEntry ->
        val scopedWords = backStackEntry.arguments?.getString(TRAINING_WORDS_ARG).orEmpty()
        TrainingGate(
            // A lesson brings its own words, so the study-set size is not what gates it.
            minimumWords = if (scopedWords.isEmpty()) minimumWords else 0,
            trainingName = trainingDisplayName(training),
            onClose = onClose,
            // Crossword can only place single words, so phrases in the study set
            // don't count toward whether there are enough words to start it.
            excludePhrases = training == LexiconDestinations.CROSSWORD,
        ) {
            screen(onComplete)
        }
    }
}

private fun Int?.orDefault(): Int = this ?: 0
