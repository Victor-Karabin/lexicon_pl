package com.lexicon.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lexicon.presentation.common.SessionResultScreen
import com.lexicon.presentation.dictation.DictationScreen
import com.lexicon.presentation.dictationpuzzle.DictationPuzzleScreen
import com.lexicon.presentation.imagetest.ImageTestScreen
import com.lexicon.presentation.main.MainScreen
import com.lexicon.presentation.main.SplashScreen
import com.lexicon.presentation.memorycards.MemoryCardsScreen
import com.lexicon.presentation.pronunciation.PronunciationScreen
import com.lexicon.presentation.puzzle.PuzzleScreen
import com.lexicon.presentation.trueorfalse.TrueOrFalseScreen
import com.lexicon.presentation.wordbuilder.WordBuilderScreen
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
            MainScreen(onTrainingSelected = { route -> navController.navigate(route) })
        }

        fun onStepSessionComplete(fromRoute: String): (Int, Int, Int) -> Unit =
            { correct, incorrect, skipped ->
                navController.navigate(LexiconDestinations.sessionResult(correct, incorrect, skipped)) {
                    popUpTo(fromRoute) { inclusive = true }
                }
            }

        composable(LexiconDestinations.DICTATION) {
            DictationScreen(onSessionComplete = onStepSessionComplete(LexiconDestinations.DICTATION))
        }
        composable(LexiconDestinations.DICTATION_PUZZLE) {
            DictationPuzzleScreen(onSessionComplete = onStepSessionComplete(LexiconDestinations.DICTATION_PUZZLE))
        }
        composable(LexiconDestinations.WORD_BUILDER) {
            WordBuilderScreen(onSessionComplete = onStepSessionComplete(LexiconDestinations.WORD_BUILDER))
        }
        composable(LexiconDestinations.TRUE_OR_FALSE) {
            TrueOrFalseScreen(onSessionComplete = onStepSessionComplete(LexiconDestinations.TRUE_OR_FALSE))
        }
        composable(LexiconDestinations.WORD_MATCH) {
            WordMatchScreen(onSessionComplete = onStepSessionComplete(LexiconDestinations.WORD_MATCH))
        }
        composable(LexiconDestinations.PRONUNCIATION_CHECK) {
            PronunciationScreen(onSessionComplete = onStepSessionComplete(LexiconDestinations.PRONUNCIATION_CHECK))
        }
        composable(LexiconDestinations.PUZZLE) {
            PuzzleScreen(onSessionComplete = onStepSessionComplete(LexiconDestinations.PUZZLE))
        }
        composable(LexiconDestinations.IMAGE_TEST) {
            ImageTestScreen(onSessionComplete = onStepSessionComplete(LexiconDestinations.IMAGE_TEST))
        }
        composable(LexiconDestinations.MEMORY_CARDS) {
            MemoryCardsScreen(onSessionComplete = onStepSessionComplete(LexiconDestinations.MEMORY_CARDS))
        }

        composable(
            route = LexiconDestinations.SESSION_RESULT,
            arguments =
                listOf(
                    navArgument("correct") { type = NavType.IntType },
                    navArgument("incorrect") { type = NavType.IntType },
                    navArgument("skipped") { type = NavType.IntType },
                ),
        ) { backStackEntry ->
            val args = backStackEntry.arguments
            SessionResultScreen(
                correct = args?.getInt("correct").orDefault(),
                incorrect = args?.getInt("incorrect").orDefault(),
                skipped = args?.getInt("skipped").orDefault(),
                onDone = {
                    navController.navigate(LexiconDestinations.MAIN) {
                        popUpTo(LexiconDestinations.MAIN) { inclusive = true }
                    }
                },
            )
        }
    }
}

private fun Int?.orDefault(): Int = this ?: 0
