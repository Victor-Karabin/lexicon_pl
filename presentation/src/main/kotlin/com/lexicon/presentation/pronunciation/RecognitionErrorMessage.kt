package com.lexicon.presentation.pronunciation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.LexiconError

@Composable
fun RecognitionErrorMessage(
    errorType: RecognitionErrorType,
    modifier: Modifier = Modifier,
) {
    Text(
        text = when (errorType) {
            RecognitionErrorType.UNAVAILABLE -> stringResource(R.string.pronunciation_recognition_unavailable)
            RecognitionErrorType.FAILED -> stringResource(R.string.pronunciation_recognition_failed)
        },
        color = LexiconError,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
    )
}
