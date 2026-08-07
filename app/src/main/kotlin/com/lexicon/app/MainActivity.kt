package com.lexicon.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.lexicon.app.navigation.LexiconNavHost
import com.lexicon.presentation.theme.LexiconAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LexiconAppTheme {
                LexiconNavHost(navController = rememberNavController())
            }
        }
    }
}
