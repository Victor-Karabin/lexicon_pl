package com.lexicon.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.lexicon.presentation.main.MainTab
import com.lexicon.presentation.main.SplashScreen
import com.lexicon.presentation.main.trainingDisplayName
import com.lexicon.presentation.memorycards.MemoryCardsScreen
import com.lexicon.presentation.mix.MixScreen
import com.lexicon.presentation.passage.PASSAGE_BANK_ARG
import com.lexicon.presentation.passage.PassageScreen
import com.lexicon.presentation.presets.CreatePresetScreen
import com.lexicon.presentation.presets.CreateWordScreen
import com.lexicon.presentation.presets.PRESET_ID_ARG
import com.lexicon.presentation.presets.PresetDetailScreen
import com.lexicon.presentation.presets.WORD_ID_ARG
import com.lexicon.presentation.program.CreateProgramScreen
import com.lexicon.presentation.program.DayCompleteScreen
import com.lexicon.presentation.program.PROGRAM_ID_ARG
import com.lexicon.presentation.program.ProgramRunStep
import com.lexicon.presentation.program.ProgramRunViewModel
import com.lexicon.presentation.program.WordCardsScreen
import com.lexicon.presentation.pronunciation.PronunciationScreen
import com.lexicon.presentation.puzzle.PuzzleScreen
import com.lexicon.presentation.trueorfalse.TrueOrFalseScreen
import com.lexicon.presentation.wordcard.WordCardScreen
import com.lexicon.presentation.wordmatch.WordMatchScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun LexiconNavHost(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = LexiconDestinations.SPLASH,
        modifier = modifier,
    ) {
        composable(LexiconDestinations.SPLASH) {
            SplashScreen(
                onFinished = {
                    navController.navigate(LexiconDestinations.main()) {
                        popUpTo(LexiconDestinations.SPLASH) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = LexiconDestinations.MAIN,
            arguments = listOf(
                navArgument(LexiconDestinations.MAIN_TAB_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { entry ->
            val requestedTab = entry.arguments
                ?.getString(LexiconDestinations.MAIN_TAB_ARG)
                ?.let { name -> MainTab.entries.firstOrNull { it.name == name } }
            MainScreen(
                initialTab = requestedTab ?: MainTab.DASHBOARD,
                onTrainingSelected = { route -> navController.navigate(route) },
                onPresetSelected = { id -> navController.navigate(LexiconDestinations.presetDetail(id)) },
                onCourseSelected = { id -> navController.navigate(LexiconDestinations.course(id)) },
                onProgramSelected = { id -> navController.navigate(LexiconDestinations.editProgram(id)) },
                onStartTraining = { training, wordIds, programId ->
                    navController.navigate(
                        LexiconDestinations.scopedTraining(training, wordIds, programId),
                    )
                },
                onOpenCards = { id -> navController.navigate(LexiconDestinations.programCards(id)) },
                onEditWord = { id -> navController.navigate(LexiconDestinations.editWord(id)) },
                onAddWord = { navController.navigate(LexiconDestinations.CREATE_WORD) },
                onAddPreset = { navController.navigate(LexiconDestinations.CREATE_PRESET) },
                onCreateProgram = { navController.navigate(LexiconDestinations.CREATE_PROGRAM) },
            )
        }

        composable(LexiconDestinations.CREATE_WORD) {
            CreateWordScreen(
                onClose = { navController.popBackStack() },
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

        composable(LexiconDestinations.CREATE_PROGRAM) {
            CreateProgramScreen(
                onClose = { navController.popBackStack() },
                onGoToVocabulary = {
                    navController.navigate(LexiconDestinations.main(MainTab.VOCABULARY)) {
                        popUpTo(LexiconDestinations.MAIN) { inclusive = true }
                    }
                },
                onCreated = { navController.popBackStack() },
            )
        }

        composable(
            route = LexiconDestinations.EDIT_PROGRAM,
            arguments = listOf(navArgument(PROGRAM_ID_ARG) { type = NavType.StringType }),
        ) {
            CreateProgramScreen(
                onClose = { navController.popBackStack() },
                onGoToVocabulary = {
                    navController.navigate(LexiconDestinations.main(MainTab.VOCABULARY)) {
                        popUpTo(LexiconDestinations.MAIN) { inclusive = true }
                    }
                },
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
            route = LexiconDestinations.PROGRAM_CARDS,
            arguments = listOf(navArgument(PROGRAM_ID_ARG) { type = NavType.StringType }),
        ) {
            WordCardsScreen(
                onClose = { navController.popBackStack() },
                onStartTraining = { training, wordIds ->
                    val route = LexiconDestinations.scopedTraining(training, wordIds.map { id -> id.value })
                    navController.navigate(route) {
                        popUpTo(LexiconDestinations.PROGRAM_CARDS) { inclusive = true }
                    }
                },
                onFinished = { navController.popBackStack() },
                onEditWord = { id -> navController.navigate(LexiconDestinations.editWord(id.value)) },
            )
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

        fun onStepSessionComplete(training: String): (String) -> (Int, Int, Int, Int) -> Unit =
            { programRun ->
                { correct, incorrect, skipped, tipsUsed ->
                    val route = LexiconDestinations.sessionResult(
                        correct = correct,
                        incorrect = incorrect,
                        skipped = skipped,
                        tipsUsed = tipsUsed,
                        programId = programRun.takeIf { it.isNotEmpty() },
                    )
                    navController.navigate(route) {
                        popUpTo(LexiconDestinations.trainingRoute(training)) { inclusive = true }
                    }
                }
            }

        val closeToMain: () -> Unit = { navController.popBackStack(LexiconDestinations.MAIN, inclusive = false) }

        val goToVocabulary: () -> Unit = {
            navController.navigate(LexiconDestinations.main(MainTab.VOCABULARY)) {
                popUpTo(LexiconDestinations.MAIN) { inclusive = true }
            }
        }

        trainingDestination(
            training = LexiconDestinations.DICTATION,
            minimumWords = TrainingRequirements.SINGLE_WORD_STEP,
            onClose = closeToMain,
            onGoToVocabulary = goToVocabulary,
            onComplete = onStepSessionComplete(LexiconDestinations.DICTATION),
        ) { onComplete ->
            DictationScreen(onSessionComplete = onComplete, onClose = closeToMain)
        }
        trainingDestination(
            training = LexiconDestinations.DICTATION_PUZZLE,
            minimumWords = TrainingRequirements.SINGLE_WORD_STEP,
            onClose = closeToMain,
            onGoToVocabulary = goToVocabulary,
            onComplete = onStepSessionComplete(LexiconDestinations.DICTATION_PUZZLE),
        ) { onComplete ->
            DictationPuzzleScreen(onSessionComplete = onComplete, onClose = closeToMain)
        }
        trainingDestination(
            training = LexiconDestinations.TRUE_OR_FALSE,
            minimumWords = TrainingRequirements.TRUE_OR_FALSE,
            onClose = closeToMain,
            onGoToVocabulary = goToVocabulary,
            onComplete = onStepSessionComplete(LexiconDestinations.TRUE_OR_FALSE),
        ) { onComplete ->
            TrueOrFalseScreen(onSessionComplete = onComplete, onClose = closeToMain)
        }
        trainingDestination(
            training = LexiconDestinations.WORD_MATCH,
            minimumWords = TrainingRequirements.WORD_MATCH,
            onClose = closeToMain,
            onGoToVocabulary = goToVocabulary,
            onComplete = onStepSessionComplete(LexiconDestinations.WORD_MATCH),
        ) { onComplete ->
            WordMatchScreen(onSessionComplete = onComplete, onClose = closeToMain)
        }
        trainingDestination(
            training = LexiconDestinations.PRONUNCIATION_CHECK,
            minimumWords = TrainingRequirements.SINGLE_WORD_STEP,
            onClose = closeToMain,
            onGoToVocabulary = goToVocabulary,
            onComplete = onStepSessionComplete(LexiconDestinations.PRONUNCIATION_CHECK),
        ) { onComplete ->
            PronunciationScreen(onSessionComplete = onComplete, onClose = closeToMain)
        }
        trainingDestination(
            training = LexiconDestinations.PUZZLE,
            minimumWords = TrainingRequirements.SINGLE_WORD_STEP,
            onClose = closeToMain,
            onGoToVocabulary = goToVocabulary,
            onComplete = onStepSessionComplete(LexiconDestinations.PUZZLE),
        ) { onComplete ->
            PuzzleScreen(onSessionComplete = onComplete, onClose = closeToMain)
        }
        trainingDestination(
            training = LexiconDestinations.IMAGE_TEST,
            minimumWords = TrainingRequirements.IMAGE_TEST,
            onClose = closeToMain,
            onGoToVocabulary = goToVocabulary,
            onComplete = onStepSessionComplete(LexiconDestinations.IMAGE_TEST),
        ) { onComplete ->
            ImageTestScreen(onSessionComplete = onComplete, onClose = closeToMain)
        }
        trainingDestination(
            training = LexiconDestinations.MEMORY_CARDS,
            minimumWords = TrainingRequirements.MEMORY_CARDS,
            onClose = closeToMain,
            onGoToVocabulary = goToVocabulary,
            onComplete = onStepSessionComplete(LexiconDestinations.MEMORY_CARDS),
        ) { onComplete ->
            MemoryCardsScreen(onSessionComplete = onComplete, onClose = closeToMain)
        }
        trainingDestination(
            training = LexiconDestinations.CROSSWORD,
            minimumWords = TrainingRequirements.CROSSWORD,
            onClose = closeToMain,
            onGoToVocabulary = goToVocabulary,
            onComplete = onStepSessionComplete(LexiconDestinations.CROSSWORD),
        ) { onComplete ->
            CrosswordScreen(onSessionComplete = onComplete, onClose = closeToMain)
        }

        composable(
            route = LexiconDestinations.trainingRoute(LexiconDestinations.WORD_CARD),
            arguments = listOf(
                navArgument(TRAINING_WORDS_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { entry ->
            val scopedWords = entry.arguments?.getString(TRAINING_WORDS_ARG).orEmpty()
            TrainingGate(
                minimumWords = if (scopedWords.isEmpty()) TrainingRequirements.SINGLE_WORD_STEP else 0,
                trainingName = trainingDisplayName(LexiconDestinations.WORD_CARD),
                onClose = closeToMain,
                onGoToVocabulary = goToVocabulary,
            ) {
                WordCardScreen(
                    onClose = closeToMain,
                    onEditWord = { id -> navController.navigate(LexiconDestinations.editWord(id)) },
                )
            }
        }
        trainingDestination(
            training = LexiconDestinations.MIX,
            minimumWords = TrainingRequirements.MIX,
            onClose = closeToMain,
            onGoToVocabulary = goToVocabulary,
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
                    navArgument(LexiconDestinations.PROGRAM_RUN_ARG) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
        ) { backStackEntry ->
            val args = backStackEntry.arguments
            val programRun = args?.getString(LexiconDestinations.PROGRAM_RUN_ARG).orEmpty()
            val run: ProgramRunViewModel = koinViewModel()
            val step by run.step.collectAsState()

            LaunchedEffect(step) {
                when (val current = step) {
                    is ProgramRunStep.Next -> {
                        val route = LexiconDestinations.scopedTraining(
                            training = current.training,
                            wordIds = current.wordIds.map { it.value },
                            programId = programRun,
                        )
                        navController.navigate(route) {
                            popUpTo(LexiconDestinations.MAIN) { inclusive = false }
                        }
                        run.onStepHandled()
                    }

                    ProgramRunStep.DayComplete -> {
                        navController.navigate(LexiconDestinations.dayComplete(programRun)) {
                            popUpTo(LexiconDestinations.MAIN) { inclusive = false }
                        }
                        run.onStepHandled()
                    }

                    else -> Unit
                }
            }

            SessionResultScreen(
                correct = args?.getInt("correct").orDefault(),
                incorrect = args?.getInt("incorrect").orDefault(),
                skipped = args?.getInt("skipped").orDefault(),
                tipsUsed = args?.getInt("tipsUsed").orDefault(),
                isProgramRun = programRun.isNotEmpty(),
                onDone = {
                    if (programRun.isEmpty()) {
                        navController.popBackStack(LexiconDestinations.MAIN, inclusive = false)
                    } else {
                        run.onTrainingFinished(programRun)
                    }
                },
            )
        }

        listOf(
            LexiconDestinations.PASSAGE_WRITE to false,
            LexiconDestinations.PASSAGE_BANK to true,
        ).forEach { (training, withWordBank) ->
            composable(
                route = LexiconDestinations.trainingRoute(training),
                arguments = listOf(
                    navArgument(TRAINING_WORDS_ARG) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument(LexiconDestinations.PROGRAM_RUN_ARG) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument(PASSAGE_BANK_ARG) {
                        type = NavType.StringType
                        defaultValue = withWordBank.toString()
                    },
                ),
            ) {
                PassageScreen(
                    withWordBank = withWordBank,
                    onSessionComplete = onStepSessionComplete(training)(""),
                    onClose = closeToMain,
                )
            }
        }

        composable(
            route = LexiconDestinations.DAY_COMPLETE,
            arguments = listOf(navArgument(PROGRAM_ID_ARG) { type = NavType.StringType }),
        ) {
            DayCompleteScreen(
                onDone = { navController.popBackStack(LexiconDestinations.MAIN, inclusive = false) },
            )
        }
    }
}

private fun NavGraphBuilder.trainingDestination(
    training: String,
    minimumWords: Int,
    onClose: () -> Unit,
    onGoToVocabulary: () -> Unit,
    onComplete: (programRun: String) -> (Int, Int, Int, Int) -> Unit,
    screen: @Composable (onComplete: (Int, Int, Int, Int) -> Unit) -> Unit,
) {
    composable(
        route = LexiconDestinations.trainingRoute(training),
        arguments = listOf(
            navArgument(TRAINING_WORDS_ARG) {
                type = NavType.StringType
                defaultValue = ""
            },
            navArgument(LexiconDestinations.PROGRAM_RUN_ARG) {
                type = NavType.StringType
                defaultValue = ""
            },
        ),
    ) { backStackEntry ->
        val scopedWords = backStackEntry.arguments?.getString(TRAINING_WORDS_ARG).orEmpty()
        val programRun = backStackEntry.arguments?.getString(LexiconDestinations.PROGRAM_RUN_ARG).orEmpty()
        TrainingGate(
            minimumWords = if (scopedWords.isEmpty()) minimumWords else 0,
            trainingName = trainingDisplayName(training),
            onClose = onClose,
            onGoToVocabulary = onGoToVocabulary,
            excludePhrases = training == LexiconDestinations.CROSSWORD,
        ) {
            screen(onComplete(programRun))
        }
    }
}

private fun Int?.orDefault(): Int = this ?: 0
