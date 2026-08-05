package com.lexicon.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lexicon.presentation.theme.Dimens

/** Shared end-of-session summary, reused by every training's result screen. */
@Composable
fun SessionResultScreen(
    correct: Int,
    incorrect: Int,
    skipped: Int,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(Dimens.spacingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Session complete", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Correct: $correct   Incorrect: $incorrect   Skipped: $skipped",
            modifier = Modifier.padding(top = Dimens.spacingMedium),
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onDone, modifier = Modifier.padding(top = Dimens.spacingLarge)) {
            Text("Done")
        }
    }
}
