package com.lexicon.pl.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lexicon.pl.presentation.dictation.DictationResultScreen
import com.lexicon.pl.presentation.dictation.DictationScreen
import com.lexicon.pl.presentation.main.MainScreen
import com.lexicon.pl.presentation.main.SplashScreen

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
            MainScreen(onDictationSelected = { navController.navigate(LexiconDestinations.DICTATION) })
        }

        composable(LexiconDestinations.DICTATION) {
            DictationScreen(
                onSessionComplete = { correct, incorrect, skipped ->
                    navController.navigate(LexiconDestinations.dictationResult(correct, incorrect, skipped)) {
                        popUpTo(LexiconDestinations.DICTATION) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = LexiconDestinations.DICTATION_RESULT,
            arguments =
                listOf(
                    navArgument("correct") { type = NavType.IntType },
                    navArgument("incorrect") { type = NavType.IntType },
                    navArgument("skipped") { type = NavType.IntType },
                ),
        ) { backStackEntry ->
            val args = backStackEntry.arguments
            DictationResultScreen(
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
