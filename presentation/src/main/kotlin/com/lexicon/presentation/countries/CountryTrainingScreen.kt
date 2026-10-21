package com.lexicon.presentation.countries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lexicon.interactors.countries.Country
import com.lexicon.interactors.countries.CountryField
import com.lexicon.interactors.course.LessonAnswerChecker
import com.lexicon.presentation.R
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.TrainingActionRow
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.conjugation.PersonFormRow
import com.lexicon.presentation.conjugation.VerbHeading
import com.lexicon.presentation.course.AnswerInputs
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.component.ProgressDots
import org.koin.androidx.compose.koinViewModel

@Composable
fun CountryTrainingScreen(
    onSessionComplete: (Int, Int, Int, Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CountryTrainingViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is SessionNavigationEvent.SessionComplete ->
                    onSessionComplete(event.correct, event.incorrect, event.skipped, event.tipsUsed)
            }
        }
    }

    CountryTrainingContent(
        uiState = uiState,
        onAnswerChanged = viewModel::onAnswerChanged,
        onCheck = viewModel::onCheck,
        onNext = viewModel::onNext,
        onSpeak = viewModel::onSpeak,
        onClose = onClose,
        modifier = modifier,
    )
}

@Composable
private fun CountryTrainingContent(
    uiState: CountryTrainingUiState,
    onAnswerChanged: (CountryField, String) -> Unit,
    onCheck: () -> Unit,
    onNext: () -> Unit,
    onSpeak: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnswerInputs(onDone = onCheck, isReady = uiState.canCheck) {
        Scaffold(
            modifier = modifier,
            topBar = { TrainingTopBar(title = stringResource(R.string.countries_title), onClose = onClose) },
        ) { padding ->
            val country = uiState.country

            when {
                uiState.isLoading ->
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }

                country == null ->
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(Dimens.spacingXl),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.countries_none),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }

                else ->
                    Column(modifier = Modifier.fillMaxSize().padding(padding).imePadding()) {
                        CountryBody(
                            uiState = uiState,
                            country = country,
                            onAnswerChanged = onAnswerChanged,
                            onSpeak = onSpeak,
                            modifier = Modifier.weight(1f),
                        )
                        TrainingActionRow(
                            onCheck = onCheck,
                            onNext = onNext,
                            awaitingNext = uiState.isAnswered,
                            checkEnabled = uiState.canCheck,
                        )
                    }
            }
        }
    }
}

@Composable
private fun CountryBody(
    uiState: CountryTrainingUiState,
    country: Country,
    onAnswerChanged: (CountryField, String) -> Unit,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(uiState.stepIndex) {
        focus.requestFocus()
        keyboard?.show()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.spacingMedium),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
    ) {
        ProgressDots(step = uiState.stepIndex, total = uiState.totalSteps, modifier = Modifier.fillMaxWidth())

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.spacingMedium),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = country.flag,
                style = MaterialTheme.typography.displayMedium,
            )
            VerbHeading(infinitive = country.english, translation = null)
        }

        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
            country.fields.forEachIndexed { index, field ->
                val typed = uiState.answers[field].orEmpty()
                val expected = LessonAnswerChecker.closest(country.answers(field), typed)
                PersonFormRow(
                    person = stringResource(field.labelId()),
                    typed = typed,
                    expected = expected,
                    verdict = uiState.verdicts[field],
                    onAnswerChanged = { onAnswerChanged(field, it) },
                    onSpeak = { onSpeak(expected) },
                    imeAction = if (index == country.fields.lastIndex) ImeAction.Done else ImeAction.Next,
                    placeholder = country.capitalEnglish.takeIf { field == CountryField.CAPITAL },
                    focusRequester = focus.takeIf { index == 0 },
                )
            }
        }
    }
}
