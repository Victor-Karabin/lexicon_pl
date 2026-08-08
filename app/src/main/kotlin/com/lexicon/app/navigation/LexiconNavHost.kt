package com.lexicon.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lexicon.presentation.common.SessionResultScreen
import com.lexicon.presentation.common.TrainingGate
import com.lexicon.presentation.common.TrainingRequirements
import com.lexicon.presentation.crossword.CrosswordScreen
import com.lexicon.presentation.dictation.DictationScreen
import com.lexicon.presentation.dictationpuzzle.DictationPuzzleScreen
import com.lexicon.presentation.imagetest.ImageTestScreen
import com.lexicon.presentation.main.MainScreen
import com.lexicon.presentation.main.SplashScreen
import com.lexicon.presentation.main.trainingDisplayName
import com.lexicon.presentation.memorycards.MemoryCardsScreen
import com.lexicon.presentation.mix.MixScreen
import com.lexicon.presentation.presets.PRESET_ID_ARG
import com.lexicon.presentation.presets.PresetDetailScreen
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
            )
        }

        composable(
            route = LexiconDestinations.PRESET_DETAIL,
            arguments = listOf(navArgument(PRESET_ID_ARG) { type = NavType.StringType }),
        ) {
            PresetDetailScreen(onClose = { navController.popBackStack() })
        }

        fun onStepSessionComplete(fromRoute: String): (Int, Int, Int, Int) -> Unit =
            { correct, incorrect, skipped, tipsUsed ->
                navController.navigate(LexiconDestinations.sessionResult(correct, incorrect, skipped, tipsUsed)) {
                    popUpTo(fromRoute) { inclusive = true }
                }
            }

        /**
         * Pops back to the existing Main entry rather than navigating to a new one. Navigating
         * would push a second Main on top of the first, so the back stack grew with every
         * training opened and closed, and each new Main started over on the first tab.
         *
         * Takes no route, because popping to Main does not depend on where it is called from.
         */
        val closeToMain: () -> Unit = { navController.popBackStack(LexiconDestinations.MAIN, inclusive = false) }

        composable(LexiconDestinations.DICTATION) {
            TrainingGate(
                minimumWords = TrainingRequirements.SINGLE_WORD_STEP,
                trainingName = trainingDisplayName(LexiconDestinations.DICTATION),
                onClose = closeToMain,
            ) {
                DictationScreen(
                    onSessionComplete = onStepSessionComplete(LexiconDestinations.DICTATION),
                    onClose = closeToMain,
                )
            }
        }
        composable(LexiconDestinations.DICTATION_PUZZLE) {
            TrainingGate(
                minimumWords = TrainingRequirements.SINGLE_WORD_STEP,
                trainingName = trainingDisplayName(LexiconDestinations.DICTATION_PUZZLE),
                onClose = closeToMain,
            ) {
                DictationPuzzleScreen(
                    onSessionComplete = onStepSessionComplete(LexiconDestinations.DICTATION_PUZZLE),
                    onClose = closeToMain,
                )
            }
        }
        composable(LexiconDestinations.TRUE_OR_FALSE) {
            TrainingGate(
                minimumWords = TrainingRequirements.TRUE_OR_FALSE,
                trainingName = trainingDisplayName(LexiconDestinations.TRUE_OR_FALSE),
                onClose = closeToMain,
            ) {
                TrueOrFalseScreen(
                    onSessionComplete = onStepSessionComplete(LexiconDestinations.TRUE_OR_FALSE),
                    onClose = closeToMain,
                )
            }
        }
        composable(LexiconDestinations.WORD_MATCH) {
            TrainingGate(
                minimumWords = TrainingRequirements.WORD_MATCH,
                trainingName = trainingDisplayName(LexiconDestinations.WORD_MATCH),
                onClose = closeToMain,
            ) {
                WordMatchScreen(
                    onSessionComplete = onStepSessionComplete(LexiconDestinations.WORD_MATCH),
                    onClose = closeToMain,
                )
            }
        }
        composable(LexiconDestinations.PRONUNCIATION_CHECK) {
            TrainingGate(
                minimumWords = TrainingRequirements.SINGLE_WORD_STEP,
                trainingName = trainingDisplayName(LexiconDestinations.PRONUNCIATION_CHECK),
                onClose = closeToMain,
            ) {
                PronunciationScreen(
                    onSessionComplete = onStepSessionComplete(LexiconDestinations.PRONUNCIATION_CHECK),
                    onClose = closeToMain,
                )
            }
        }
        composable(LexiconDestinations.PUZZLE) {
            TrainingGate(
                minimumWords = TrainingRequirements.SINGLE_WORD_STEP,
                trainingName = trainingDisplayName(LexiconDestinations.PUZZLE),
                onClose = closeToMain,
            ) {
                PuzzleScreen(
                    onSessionComplete = onStepSessionComplete(LexiconDestinations.PUZZLE),
                    onClose = closeToMain,
                )
            }
        }
        composable(LexiconDestinations.IMAGE_TEST) {
            TrainingGate(
                minimumWords = TrainingRequirements.IMAGE_TEST,
                trainingName = trainingDisplayName(LexiconDestinations.IMAGE_TEST),
                onClose = closeToMain,
            ) {
                ImageTestScreen(
                    onSessionComplete = onStepSessionComplete(LexiconDestinations.IMAGE_TEST),
                    onClose = closeToMain,
                )
            }
        }
        composable(LexiconDestinations.MEMORY_CARDS) {
            TrainingGate(
                minimumWords = TrainingRequirements.MEMORY_CARDS,
                trainingName = trainingDisplayName(LexiconDestinations.MEMORY_CARDS),
                onClose = closeToMain,
            ) {
                MemoryCardsScreen(
                    onSessionComplete = onStepSessionComplete(LexiconDestinations.MEMORY_CARDS),
                    onClose = closeToMain,
                )
            }
        }
        composable(LexiconDestinations.CROSSWORD) {
            TrainingGate(
                minimumWords = TrainingRequirements.CROSSWORD,
                trainingName = trainingDisplayName(LexiconDestinations.CROSSWORD),
                onClose = closeToMain,
            ) {
                CrosswordScreen(
                    onSessionComplete = onStepSessionComplete(LexiconDestinations.CROSSWORD),
                    onClose = closeToMain,
                )
            }
        }

        composable(LexiconDestinations.MIX) {
            TrainingGate(
                minimumWords = TrainingRequirements.MIX,
                trainingName = trainingDisplayName(LexiconDestinations.MIX),
                onClose = closeToMain,
            ) {
                MixScreen(
                    onSessionComplete = onStepSessionComplete(LexiconDestinations.MIX),
                    onClose = closeToMain,
                )
            }
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
                // Same reason as onClose: return to the Main that is already there.
                onDone = { navController.popBackStack(LexiconDestinations.MAIN, inclusive = false) },
            )
        }
    }
}

private fun Int?.orDefault(): Int = this ?: 0
