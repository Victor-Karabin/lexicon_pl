package com.lexicon.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.lexicon.interactors.settings.AppSettings
import com.lexicon.interactors.settings.ThemeMode
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconTheme
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val settings by viewModel.uiState.collectAsState()

    SettingsScreenContent(
        settings = settings,
        onThemeModeSelected = viewModel::onThemeModeSelected,
        onStepCountChanged = viewModel::onStepCountChanged,
        modifier = modifier,
    )
}

@Composable
private fun SettingsScreenContent(
    settings: AppSettings,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onStepCountChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.spacingMedium),
    ) {
        Text(
            text = stringResource(R.string.settings_appearance),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        Column(modifier = Modifier.selectableGroup().padding(top = Dimens.spacingSmall)) {
            ThemeMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = mode == settings.themeMode,
                            onClick = { onThemeModeSelected(mode) },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = Dimens.spacingSmall),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = mode == settings.themeMode, onClick = null)
                    Text(
                        text = stringResource(mode.labelRes()),
                        modifier = Modifier.padding(start = Dimens.spacingMedium),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.spacingMedium))

        Text(
            text = stringResource(R.string.settings_training),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingSmall),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = stringResource(R.string.settings_step_count), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = settings.stepCount.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        Slider(
            value = settings.stepCount.toFloat(),
            onValueChange = { onStepCountChanged(it.roundToInt()) },
            valueRange = AppSettings.MIN_STEP_COUNT.toFloat()..AppSettings.MAX_STEP_COUNT.toFloat(),
            steps = AppSettings.MAX_STEP_COUNT - AppSettings.MIN_STEP_COUNT - 1,
        )

        Text(
            text = stringResource(R.string.settings_step_count_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun ThemeMode.labelRes(): Int =
    when (this) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    }

@LightDarkPreview
@Composable
private fun SettingsScreenPreview() {
    LexiconTheme {
        SettingsScreenContent(
            settings = AppSettings(themeMode = ThemeMode.SYSTEM, stepCount = 10),
            onThemeModeSelected = {},
            onStepCountChanged = {},
        )
    }
}

@LightDarkPreview
@Composable
private fun SettingsScreenDarkSelectedPreview() {
    LexiconTheme {
        SettingsScreenContent(
            settings = AppSettings(themeMode = ThemeMode.DARK, stepCount = AppSettings.MAX_STEP_COUNT),
            onThemeModeSelected = {},
            onStepCountChanged = {},
        )
    }
}

@LightDarkPreview
@Composable
private fun SettingsScreenMinimumStepsPreview() {
    LexiconTheme {
        SettingsScreenContent(
            settings = AppSettings(themeMode = ThemeMode.LIGHT, stepCount = AppSettings.MIN_STEP_COUNT),
            onThemeModeSelected = {},
            onStepCountChanged = {},
        )
    }
}
