package com.lexicon.presentation.common

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

/** Shared top bar for every training screen: a title plus a close action back to the home screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingTopBar(
    title: String,
    onClose: () -> Unit,
) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Text("✕")
            }
        },
    )
}
