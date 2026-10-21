package com.lexicon.presentation.numbers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lexicon.interactors.numbers.NumberSelection
import com.lexicon.presentation.R
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.theme.Dimens
import org.koin.androidx.compose.koinViewModel

@Composable
fun NumberCourseScreen(
    onStarted: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NumberCourseViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.started.collect { onStarted() } }

    NumberCourseContent(
        uiState = uiState,
        onSpecChanged = viewModel::onSpecChanged,
        onStart = viewModel::onStart,
        onClose = onClose,
        modifier = modifier,
    )
}

@Composable
private fun NumberCourseContent(
    uiState: NumberCourseUiState,
    onSpecChanged: (String) -> Unit,
    onStart: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TrainingTopBar(title = stringResource(R.string.numbers_title), onClose = onClose) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(Dimens.spacingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
        ) {
            Text(
                text = stringResource(R.string.numbers_setup_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val selection = uiState.selection
            val showError = uiState.spec.isNotBlank() && selection !is NumberSelection.Valid
            OutlinedTextField(
                value = uiState.spec,
                onValueChange = onSpecChanged,
                label = { Text(stringResource(R.string.numbers_input_label)) },
                placeholder = { Text(stringResource(R.string.numbers_input_placeholder)) },
                supportingText = { selectionText(selection)?.let { Text(it) } },
                isError = showError,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = onStart,
                enabled = uiState.canStart,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.course_start))
            }
        }
    }
}

@Composable
private fun selectionText(selection: NumberSelection): String? =
    when (selection) {
        is NumberSelection.Valid -> stringResource(R.string.numbers_count, selection.numbers.size)
        is NumberSelection.Unreadable -> stringResource(R.string.numbers_error_unreadable, selection.part)
        NumberSelection.TooLarge -> stringResource(R.string.numbers_error_too_large)
        NumberSelection.TooMany -> stringResource(R.string.numbers_error_too_many)
        NumberSelection.Empty -> stringResource(R.string.numbers_error_empty)
    }
