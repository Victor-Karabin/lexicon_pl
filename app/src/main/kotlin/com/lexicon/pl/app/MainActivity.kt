package com.lexicon.pl.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.lexicon.pl.app.navigation.LexiconNavHost
import com.lexicon.pl.presentation.theme.LexiconTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LexiconTheme {
                LexiconNavHost(navController = rememberNavController())
            }
        }
    }
}
