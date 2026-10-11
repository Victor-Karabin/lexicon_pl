package com.lexicon.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.lexicon.app.navigation.LexiconNavHost
import com.lexicon.presentation.common.TrackTextInput
import com.lexicon.presentation.theme.LexiconAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LexiconAppTheme {
                TrackTextInput {
                    LexiconNavHost(
                        navController = rememberNavController(),
                        modifier = Modifier.fillMaxSize().imePadding(),
                    )
                }
            }
        }
    }
}
