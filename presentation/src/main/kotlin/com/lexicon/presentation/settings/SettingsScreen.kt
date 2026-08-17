package com.lexicon.presentation.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lexicon.android.SpeechVoice
import com.lexicon.interactors.settings.AppSettings
import com.lexicon.interactors.settings.ThemeMode
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconTheme
import com.lexicon.presentation.theme.component.GradientTile
import com.lexicon.presentation.theme.component.Medallion
import com.lexicon.presentation.theme.component.MedallionIcon
import com.lexicon.presentation.theme.component.TileSkin
import com.lexicon.presentation.theme.component.muted
import com.lexicon.presentation.theme.component.tileSkin
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

private val HeadingIconSize = 36.dp

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val settings by viewModel.uiState.collectAsState()
    val voices by viewModel.voices.collectAsState()

    SettingsScreenContent(
        settings = settings,
        voices = voices,
        onThemeModeSelected = viewModel::onThemeModeSelected,
        onStepCountChanged = viewModel::onStepCountChanged,
        onVoiceSelected = viewModel::onVoiceSelected,
        modifier = modifier,
    )
}

@Composable
private fun SettingsScreenContent(
    settings: AppSettings,
    voices: List<SpeechVoice>,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onStepCountChanged: (Int) -> Unit,
    onVoiceSelected: (SpeechVoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.spacingMedium),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
    ) {
        val skin = tileSkin()

        GradientTile(skin = skin) {
            SettingHeading(
                icon = Icons.Default.Palette,
                text = stringResource(R.string.settings_appearance),
                skin = skin,
            )

            Column(modifier = Modifier.selectableGroup()) {
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
                            color = skin.onTile,
                        )
                    }
                }
            }
        }

        GradientTile(skin = skin) {
            SettingHeading(
                icon = Icons.Default.FitnessCenter,
                text = stringResource(R.string.settings_training),
                skin = skin,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.settings_step_count),
                    style = MaterialTheme.typography.bodyLarge,
                    color = skin.onTile,
                )
                Text(
                    text = settings.stepCount.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = skin.onTile,
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
                color = skin.muted(),
            )
        }

        if (voices.isNotEmpty()) {
            GradientTile(skin = skin) {
                SettingHeading(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    text = stringResource(R.string.settings_voice),
                    skin = skin,
                )

                voices.forEach { voice ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onVoiceSelected(voice) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = settings.voiceId == voice.id,
                            onClick = { onVoiceSelected(voice) },
                        )
                        Text(
                            text = voice.displayName,
                            modifier = Modifier.padding(start = Dimens.spacingMedium),
                            style = MaterialTheme.typography.bodyLarge,
                            color = skin.onTile,
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.settings_voice_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = skin.muted(),
                )
            }
        }
    }
}

@Composable
private fun SettingHeading(
    icon: ImageVector,
    text: String,
    skin: TileSkin,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Medallion(skin = skin, size = HeadingIconSize) { MedallionIcon(icon, skin) }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = skin.onTile,
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
            voices = emptyList(),
            onThemeModeSelected = {},
            onVoiceSelected = {},
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
            voices = emptyList(),
            onThemeModeSelected = {},
            onVoiceSelected = {},
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
            voices = emptyList(),
            onThemeModeSelected = {},
            onVoiceSelected = {},
            onStepCountChanged = {},
        )
    }
}
