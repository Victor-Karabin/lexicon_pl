package com.lexicon.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Named answer-state tokens from the design handoff (DESIGN.md §3). Not yet wired into any live
 * screen — those still color state directly via [LexiconSuccess]/[LexiconError].
 */
object LexiconSemanticColors {
    val answerCorrect: Color get() = LexiconSuccessContainer
    val answerWrong: Color get() = LexiconErrorContainer

    val answerNeutral: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh

    val progressTrack: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh
}
