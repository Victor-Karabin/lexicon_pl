package com.lexicon.pl.presentation.dictation

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
import com.lexicon.pl.presentation.theme.Dimens

@Composable
fun DictationResultScreen(
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
