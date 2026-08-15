package com.lexicon.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.lexicon.app.navigation.LexiconNavHost
import com.lexicon.presentation.theme.LexiconAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LexiconAppTheme {
                // The whole app gives way to the keyboard. From API 35 the window no
                // longer resizes itself for it, so without this the keyboard is drawn
                // over the bottom of every screen — on the crossword it covered the
                // clue being answered, which is the one thing needed while typing.
                LexiconNavHost(
                    navController = rememberNavController(),
                    modifier = Modifier.fillMaxSize().imePadding(),
                )
            }
        }
    }
}
