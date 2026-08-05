package com.lexicon.pl.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lexicon.pl.presentation.common.SessionResultScreen
import com.lexicon.pl.presentation.dictation.DictationScreen
import com.lexicon.pl.presentation.dictationpuzzle.DictationPuzzleScreen
import com.lexicon.pl.presentation.main.MainScreen
import com.lexicon.pl.presentation.main.SplashScreen
import com.lexicon.pl.presentation.pronunciation.PronunciationScreen
import com.lexicon.pl.presentation.trueorfalse.TrueOrFalseScreen
import com.lexicon.pl.presentation.wordbuilder.WordBuilderScreen
import com.lexicon.pl.presentation.wordmatch.WordMatchScreen

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
