package com.lexicon.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lexicon.model.training.TrainingType
import com.lexicon.presentation.common.LocalCourseReset
import com.lexicon.presentation.common.SessionResultScreen
import com.lexicon.presentation.common.TRAINING_WORDS_ARG
import com.lexicon.presentation.common.TrainingGate
import com.lexicon.presentation.conjugation.CONJUGATION_COURSE_ARG
import com.lexicon.presentation.conjugation.ConjugationScreen
import com.lexicon.presentation.conjugation.VerbSelectionScreen
import com.lexicon.presentation.course.COURSE_ID_ARG
import com.lexicon.presentation.course.CourseDetailScreen
import com.lexicon.presentation.course.EXERCISE_ID_ARG
import com.lexicon.presentation.course.ExerciseScreen
import com.lexicon.presentation.course.LESSON_ID_ARG
import com.lexicon.presentation.course.LessonScreen
import com.lexicon.presentation.crossword.CrosswordScreen
import com.lexicon.presentation.dictation.DictationScreen
import com.lexicon.presentation.dictationpuzzle.DictationPuzzleScreen
import com.lexicon.presentation.fillword.FillwordScreen
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
import com.lexicon.presentation.pronunciation.PRONUNCIATION_SENTENCES_ARG
import com.lexicon.presentation.pronunciation.PronunciationScreen
import com.lexicon.presentation.puzzle.PuzzleScreen
import com.lexicon.presentation.review.ReviewWordsScreen
import com.lexicon.presentation.trueorfalse.TrueOrFalseScreen
import com.lexicon.presentation.vocabularycourse.CourseRunStep
import com.lexicon.presentation.vocabularycourse.CourseRunViewModel
import com.lexicon.presentation.vocabularycourse.CourseSettingsScreen
import com.lexicon.presentation.vocabularycourse.WordCardsScreen
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
                onOpenCourseSettings = { navController.navigate(LexiconDestinations.COURSE_SETTINGS) },
                onStartTraining = { training, wordIds ->
                    navController.navigate(LexiconDestinations.scopedTraining(training, wordIds, inCourse = true))
                },
                onOpenCards = { navController.navigate(LexiconDestinations.COURSE_CARDS) },
                onEditWord = { id -> navController.navigate(LexiconDestinations.editWord(id)) },
                onAddWord = { navController.navigate(LexiconDestinations.CREATE_WORD) },
                onAddPreset = { navController.navigate(LexiconDestinations.CREATE_PRESET) },
                onConjugationSelected = { navController.navigate(LexiconDestinations.CONJUGATION_VERBS) },
                onTrainConjugation = { navController.navigate(LexiconDestinations.conjugationCourse(it)) },
                onReviewWords = { navController.navigate(LexiconDestinations.REVIEW_WORDS) },
            )
        }

        composable(LexiconDestinations.REVIEW_WORDS) {
            ReviewWordsScreen(
                onClose = { navController.popBackStack() },
                onEditWord = { id -> navController.navigate(LexiconDestinations.editWord(id.value)) },
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

        composable(LexiconDestinations.COURSE_SETTINGS) {
            CourseSettingsScreen(onClose = { navController.popBackStack() })
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

        composable(LexiconDestinations.COURSE_CARDS) {
            WordCardsScreen(
                onClose = { navController.popBackStack() },
                onStartTraining = { training, wordIds ->
                    val route = LexiconDestinations.scopedTraining(
                        training = training,
                        wordIds = wordIds.map { id -> id.value },
                        inCourse = true,
                    )
                    navController.navigate(route) {
                        popUpTo(LexiconDestinations.COURSE_CARDS) { inclusive = true }
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
            { courseRun ->
                { correct, incorrect, skipped, tipsUsed ->
                    val route = LexiconDestinations.sessionResult(
                        correct = correct,
                        incorrect = incorrect,
                        skipped = skipped,
                        tipsUsed = tipsUsed,
                        inCourse = courseRun.isNotEmpty(),
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
            navController = navController,
            minimumWords = TrainingType.DICTATION.minimumWords,
            onClose = closeToMain,
            onGoToVocabulary = goToVocabulary,
            onComplete = onStepSessionComplete(LexiconDestinations.DICTATION),
        ) { onComplete ->
            DictationScreen(onSessionComplete = onComplete, onClose = closeToMain)
        }
        trainingDestination(
            training = LexiconDestinations.DICTATION_PUZZLE,
            navController = navController,
            minimumWords = TrainingType.DICTATION.minimumWords,
            onClose = closeToMain,
            onGoToVocabulary = goToVocabulary,
            onComplete = onStepSessionComplete(LexiconDestinations.DICTATION_PUZZLE),
        ) { onComplete ->
            DictationPuzzleScreen(onSessionComplete = onComplete, onClose = closeToMain)
        }
        trainingDestination(
            training = LexiconDestinations.TRUE_OR_FALSE,
            navController = navController,
            minimumWords = TrainingType.TRUE_OR_FALSE.minimumWords,
            onClose = closeToMain,
            onGoToVocabulary = goToVocabulary,
            onComplete = onStepSessionComplete(LexiconDestinations.TRUE_OR_FALSE),
        ) { onComplete ->
            TrueOrFalseScreen(onSessionComplete = onComplete, onClose = closeToMain)
        }
        trainingDestination(
            training = LexiconDestinations.WORD_MATCH,
            navController = navController,
            minimumWords = TrainingType.WORD_MATCH.minimumWords,
            onClose = closeToMain,
            onGoToVocabulary = goToVocabulary,
            onComplete = onStepSessionComplete(LexiconDestinations.WORD_MATCH),
        ) { onComplete ->
            WordMatchScreen(onSessionComplete = onComplete, onClose = closeToMain)
        }
        trainingDestination(
            training = LexiconDestinations.PRONUNCIATION_CHECK,
            navController = navController,
            minimumWords = TrainingType.DICTATION.minimumWords,
            onClose = closeToMain,
            onGoToVocabulary = goToVocabulary,
            onComplete = onStepSessionComplete(LexiconDestinations.PRONUNCIATION_CHECK),
        ) { onComplete ->
            PronunciationScreen(onSessionComplete = onComplete, onClose = closeToMain)
        }
        trainingDestination(
            training = LexiconDestinations.PUZZLE,
            navController = navController,
            minimumWords = TrainingType.DICTATION.minimumWords,
            onClose = closeToMain,
            onGoToVocabulary = goToVocabulary,
            onComplete = onStepSessionComplete(LexiconDestinations.PUZZLE),
        ) { onComplete ->
            PuzzleScreen(onSessionComplete = onComplete, onClose = closeToMain)
        }
        trainingDestination(
            training = LexiconDestinations.IMAGE_TEST,
            navController = navController,
            minimumWords = TrainingType.IMAGE_TEST.minimumWords,
            onClose = closeToMain,
            onGoToVocabulary = goToVocabulary,
            onComplete = onStepSessionComplete(LexiconDestinations.IMAGE_TEST),
        ) { onComplete ->
            ImageTestScreen(onSessionComplete = onComplete, onClose = closeToMain)
        }
        trainingDestination(
            training = LexiconDestinations.MEMORY_CARDS,
            navController = navController,
            minimumWords = TrainingType.MEMORY_CARDS.minimumWords,
            onClose = closeToMain,
            onGoToVocabulary = goToVocabulary,
            onComplete = onStepSessionComplete(LexiconDestinations.MEMORY_CARDS),
        ) { onComplete ->
            MemoryCardsScreen(onSessionComplete = onComplete, onClose = closeToMain)
        }
        trainingDestination(
            training = LexiconDestinations.CROSSWORD,
            navController = navController,
            minimumWords = TrainingType.CROSSWORD.minimumWords,
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
                navArgument(LexiconDestinations.COURSE_RUN_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { entry ->
            val scopedWords = entry.arguments?.getString(TRAINING_WORDS_ARG).orEmpty()
            val courseRun = entry.arguments?.getString(LexiconDestinations.COURSE_RUN_ARG).orEmpty()

            CourseRunScope(courseRun = courseRun, navController = navController) { run ->
                TrainingGate(
                    minimumWords = if (scopedWords.isEmpty()) TrainingType.DICTATION.minimumWords else 0,
                    trainingName = trainingDisplayName(LexiconDestinations.WORD_CARD),
                    onClose = closeToMain,
                    onGoToVocabulary = goToVocabulary,
                ) {
                    WordCardScreen(
                        onClose = closeToMain,
                        onFinished = {
                            if (courseRun.isEmpty()) closeToMain() else run.onTrainingFinished()
                        },
                        onEditWord = { id -> navController.navigate(LexiconDestinations.editWord(id)) },
                    )
                }
            }
        }
        trainingDestination(
            training = LexiconDestinations.MIX,
            navController = navController,
            minimumWords = TrainingType.MIX.minimumWords,
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
                    navArgument(LexiconDestinations.COURSE_RUN_ARG) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
        ) { backStackEntry ->
            val args = backStackEntry.arguments
            val courseRun = args?.getString(LexiconDestinations.COURSE_RUN_ARG).orEmpty()

            CourseRunScope(courseRun = courseRun, navController = navController) { run ->
                SessionResultScreen(
                    correct = args?.getInt("correct").orDefault(),
                    incorrect = args?.getInt("incorrect").orDefault(),
                    skipped = args?.getInt("skipped").orDefault(),
                    tipsUsed = args?.getInt("tipsUsed").orDefault(),
                    isCourseRun = courseRun.isNotEmpty(),
                    onDone = {
                        if (courseRun.isEmpty()) {
                            navController.popBackStack(LexiconDestinations.MAIN, inclusive = false)
                        } else {
                            run.onTrainingFinished()
                        }
                    },
                )
            }
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
                    navArgument(LexiconDestinations.COURSE_RUN_ARG) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument(PASSAGE_BANK_ARG) {
                        type = NavType.StringType
                        defaultValue = withWordBank.toString()
                    },
                ),
            ) { entry ->
                val courseRun = entry.arguments?.getString(LexiconDestinations.COURSE_RUN_ARG).orEmpty()
                CourseRunScope(courseRun = courseRun, navController = navController) {
                    PassageScreen(
                        withWordBank = withWordBank,
                        onSessionComplete = onStepSessionComplete(training)(courseRun),
                        onClose = closeToMain,
                    )
                }
            }
        }

        composable(
            route = LexiconDestinations.trainingRoute(LexiconDestinations.PRONUNCIATION_SENTENCES),
            arguments = listOf(
                navArgument(TRAINING_WORDS_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(LexiconDestinations.COURSE_RUN_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(PRONUNCIATION_SENTENCES_ARG) {
                    type = NavType.StringType
                    defaultValue = true.toString()
                },
            ),
        ) { entry ->
            val courseRun = entry.arguments?.getString(LexiconDestinations.COURSE_RUN_ARG).orEmpty()
            CourseRunScope(courseRun = courseRun, navController = navController) {
                PronunciationScreen(
                    readsSentences = true,
                    onSessionComplete = onStepSessionComplete(LexiconDestinations.PRONUNCIATION_SENTENCES)(courseRun),
                    onClose = closeToMain,
                )
            }
        }

        trainingDestination(
            training = LexiconDestinations.FILLWORD,
            navController = navController,
            minimumWords = TrainingType.DICTATION.minimumWords,
            onClose = closeToMain,
            onGoToVocabulary = goToVocabulary,
            onComplete = onStepSessionComplete(LexiconDestinations.FILLWORD),
        ) { onComplete ->
            FillwordScreen(onSessionComplete = onComplete, onClose = closeToMain)
        }

        composable(route = LexiconDestinations.CONJUGATION_VERBS) {
            VerbSelectionScreen(
                onContinue = { navController.popBackStack() },
                onClose = closeToMain,
            )
        }

        composable(
            route = LexiconDestinations.CONJUGATION,
            arguments = listOf(navArgument(CONJUGATION_COURSE_ARG) { type = NavType.StringType }),
        ) {
            ConjugationScreen(
                onSessionComplete = onStepSessionComplete(LexiconDestinations.CONJUGATION)(""),
                onClose = closeToMain,
            )
        }
    }
}

@Composable
private fun CourseRunScope(
    courseRun: String,
    navController: NavHostController,
    content: @Composable (run: CourseRunViewModel) -> Unit,
) {
    val run: CourseRunViewModel = koinViewModel()
    val step by run.step.collectAsStateWithLifecycle()

    LaunchedEffect(step) {
        when (val current = step) {
            is CourseRunStep.Next -> {
                val route = LexiconDestinations.scopedTraining(
                    training = current.training.id,
                    wordIds = current.wordIds.map { it.value },
                    inCourse = true,
                )
                navController.navigate(route) {
                    popUpTo(LexiconDestinations.MAIN) { inclusive = false }
                }
                run.onStepHandled()
            }

            CourseRunStep.Cards -> {
                navController.navigate(LexiconDestinations.COURSE_CARDS) {
                    popUpTo(LexiconDestinations.MAIN) { inclusive = false }
                }
                run.onStepHandled()
            }

            CourseRunStep.NothingToPractise -> {
                navController.popBackStack(LexiconDestinations.MAIN, inclusive = false)
                run.onStepHandled()
            }

            else -> Unit
        }
    }

    CompositionLocalProvider(LocalCourseReset provides if (courseRun.isEmpty()) null else run::onReset) {
        content(run)
    }
}

private fun NavGraphBuilder.trainingDestination(
    training: String,
    navController: NavHostController,
    minimumWords: Int,
    onClose: () -> Unit,
    onGoToVocabulary: () -> Unit,
    onComplete: (courseRun: String) -> (Int, Int, Int, Int) -> Unit,
    screen: @Composable (onComplete: (Int, Int, Int, Int) -> Unit) -> Unit,
) {
    composable(
        route = LexiconDestinations.trainingRoute(training),
        arguments = listOf(
            navArgument(TRAINING_WORDS_ARG) {
                type = NavType.StringType
                defaultValue = ""
            },
            navArgument(LexiconDestinations.COURSE_RUN_ARG) {
                type = NavType.StringType
                defaultValue = ""
            },
        ),
    ) { backStackEntry ->
        val scopedWords = backStackEntry.arguments?.getString(TRAINING_WORDS_ARG).orEmpty()
        val courseRun = backStackEntry.arguments?.getString(LexiconDestinations.COURSE_RUN_ARG).orEmpty()
        CourseRunScope(courseRun = courseRun, navController = navController) {
            TrainingGate(
                minimumWords = if (scopedWords.isEmpty()) minimumWords else 0,
                trainingName = trainingDisplayName(training),
                onClose = onClose,
                onGoToVocabulary = onGoToVocabulary,
                excludePhrases = training == LexiconDestinations.CROSSWORD,
            ) {
                screen(onComplete(courseRun))
            }
        }
    }
}

private fun Int?.orDefault(): Int = this ?: 0
