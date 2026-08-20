package com.lexicon.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconTheme

val DeleteActionWidth = 88.dp

private val DeleteActionRadius = 12.dp

private val PreviewRowHeight = 76.dp

@Composable
fun DeleteAction(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = LexiconError,
        shape = RoundedCornerShape(topEnd = DeleteActionRadius, bottomEnd = DeleteActionRadius),
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
            Text(
                text = stringResource(R.string.vocabulary_delete),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
            )
        }
    }
}

@LightDarkPreview
@Composable
private fun DeleteActionPreview() {
    LexiconTheme {
        Box(modifier = Modifier.width(DeleteActionWidth).height(PreviewRowHeight)) {
            DeleteAction(onClick = {})
        }
    }
}
