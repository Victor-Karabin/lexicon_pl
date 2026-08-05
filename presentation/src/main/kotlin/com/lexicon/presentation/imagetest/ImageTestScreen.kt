package com.lexicon.presentation.imagetest

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconSuccess

private val ImageHeight = 180.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageTestScreen(
    onSessionComplete: (correct: Int, incorrect: Int, skipped: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImageTestViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is SessionNavigationEvent.SessionComplete ->
                    onSessionComplete(event.correct, event.incorrect, event.skipped)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Image Test") }) },
    ) { padding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(Dimens.spacingMedium)) {
            LinearProgressIndicator(
                progress = { (uiState.stepIndex + 1f) / uiState.totalSteps },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "${uiState.stepIndex + 1} / ${uiState.totalSteps}",
                modifier = Modifier.padding(top = Dimens.spacingSmall),
                style = MaterialTheme.typography.labelMedium,
            )

            Box(modifier = Modifier.fillMaxWidth().height(ImageHeight).padding(top = Dimens.spacingMedium)) {
                if (uiState.imageUrl == null) {
                    Text(uiState.clueText, style = MaterialTheme.typography.headlineSmall)
                } else {
                    SubcomposeAsyncImage(
                        model = uiState.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                        loading = { CircularProgressIndicator() },
                        error = { Text(uiState.clueText, style = MaterialTheme.typography.headlineSmall) },
                    )
                }
            }

            Column(modifier = Modifier.padding(top = Dimens.spacingMedium)) {
                uiState.options.forEach { option ->
                    OptionRow(
                        text = option,
                        isSelected = option == uiState.selectedOption,
                        isCorrect = uiState.correctOption?.let { it == option },
                        enabled = uiState.isEditable,
                        onClick = { viewModel.onOptionSelected(option) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingLarge),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
            ) {
                TextButton(onClick = viewModel::onSkip, enabled = uiState.canSkip) {
                    Text("Skip")
                }
                Button(
                    onClick = { if (uiState.awaitingNext) viewModel.onNext() else viewModel.onCheck() },
                    enabled = uiState.awaitingNext || uiState.canCheck,
                ) {
                    Text(if (uiState.awaitingNext) "Next" else "Check")
                }
            }
        }
    }
}

@Composable
private fun OptionRow(
    text: String,
    isSelected: Boolean,
    isCorrect: Boolean?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val background =
        when {
            isCorrect == true -> LexiconSuccess
            isCorrect == false && isSelected -> LexiconError
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    val textColor = if (isCorrect != null || isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.spacingTiny)
                .background(background, RoundedCornerShape(Dimens.spacingSmall))
                .clickable(enabled = enabled, onClick = onClick)
                .padding(Dimens.spacingSmall),
    ) {
        Text(text, color = textColor)
    }
}
