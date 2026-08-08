package com.lexicon.presentation.imagetest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexicon.presentation.R
import com.lexicon.presentation.common.AnswerOptionList
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.ClueImage
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.TrainingActionRow
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageTestScreen(
    onSessionComplete: (correct: Int, incorrect: Int, skipped: Int, tipsUsed: Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImageTestViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is SessionNavigationEvent.SessionComplete ->
                    onSessionComplete(event.correct, event.incorrect, event.skipped, event.tipsUsed)
            }
        }
    }

    ImageTestScreenContent(
        uiState = uiState,
        onClose = onClose,
        onOptionSelected = viewModel::onOptionSelected,
        onSkip = viewModel::onSkip,
        onCheck = viewModel::onCheck,
        onNext = viewModel::onNext,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageTestScreenContent(
    uiState: ImageTestUiState,
    onClose: () -> Unit,
    onOptionSelected: (String) -> Unit,
    onSkip: () -> Unit,
    onCheck: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TrainingTopBar(title = stringResource(R.string.image_test_title), onClose = onClose) },
    ) { padding ->
        when (uiState) {
            is ImageTestUiState.Loading ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            is ImageTestUiState.Loaded ->
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(Dimens.spacingMedium),
                    ) {
                        LinearProgressIndicator(
                            progress = { (uiState.stepIndex + 1f) / uiState.totalSteps },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = "${uiState.stepIndex + 1} / ${uiState.totalSteps}",
                            modifier = Modifier.padding(top = Dimens.spacingSmall),
                            style = MaterialTheme.typography.labelMedium,
                        )

                        ClueImage(
                            imageUrl = uiState.imageUrl,
                            fallbackText = uiState.clueText,
                            modifier = Modifier.padding(top = Dimens.spacingMedium),
                        )

                        AnswerOptionList(
                            options = uiState.options,
                            selectedOption = uiState.selectedOption,
                            correctOption = uiState.correctOption,
                            enabled = uiState.isEditable,
                            onOptionSelected = onOptionSelected,
                            modifier = Modifier.padding(top = Dimens.spacingMedium),
                        )
                    }

                    TrainingActionRow(
                        onCheck = onCheck,
                        onNext = onNext,
                        awaitingNext = uiState.awaitingNext,
                        checkEnabled = uiState.canCheck,
                        onSkip = onSkip.takeIf { uiState.canSkip },
                    )
                }
        }
    }
}

@LightDarkPreview
@Composable
private fun ImageTestScreenPreview() {
    LexiconTheme {
        ImageTestScreenContent(
            uiState =
                ImageTestUiState.Loaded(
                    stepIndex = 2,
                    totalSteps = 10,
                    clueText = "praca",
                    options = listOf("work", "house", "dog", "cat", "tree", "book"),
                    selectedOption = "work",
                    answerState = AnswerState.Unanswered,
                ),
            onClose = {},
            onOptionSelected = {},
            onSkip = {},
            onCheck = {},
            onNext = {},
        )
    }
}

@LightDarkPreview
@Composable
private fun ImageTestScreenCorrectPreview() {
    LexiconTheme {
        ImageTestScreenContent(
            uiState =
                ImageTestUiState.Loaded(
                    stepIndex = 2,
                    totalSteps = 10,
                    clueText = "praca",
                    options = listOf("work", "house", "dog", "cat", "tree", "book"),
                    selectedOption = "work",
                    correctOption = "work",
                    answerState = AnswerState.Correct,
                ),
            onClose = {},
            onOptionSelected = {},
            onSkip = {},
            onCheck = {},
            onNext = {},
        )
    }
}

@LightDarkPreview
@Composable
private fun ImageTestScreenIncorrectPreview() {
    LexiconTheme {
        ImageTestScreenContent(
            uiState =
                ImageTestUiState.Loaded(
                    stepIndex = 2,
                    totalSteps = 10,
                    clueText = "praca",
                    options = listOf("work", "house", "dog", "cat", "tree", "book"),
                    selectedOption = "house",
                    correctOption = "work",
                    answerState = AnswerState.Incorrect("work"),
                ),
            onClose = {},
            onOptionSelected = {},
            onSkip = {},
            onCheck = {},
            onNext = {},
        )
    }
}
