package com.lexicon.presentation.presets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lexicon.interactors.presets.PresetStudySetState
import com.lexicon.interactors.presets.VocabularyPreset
import com.lexicon.interactors.presets.resolve
import com.lexicon.interactors.presets.wordCount
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.component.Medallion
import com.lexicon.presentation.theme.component.MedallionIcon
import com.lexicon.presentation.theme.component.StatChip
import com.lexicon.presentation.theme.component.TileSkin
import com.lexicon.presentation.theme.component.accentTileSkin
import com.lexicon.presentation.theme.component.muted
import java.text.NumberFormat

private val PresetMedallionSize = 48.dp

@Composable
fun presetTileSkin(preset: VocabularyPreset): TileSkin {
    val accent = preset.accentColor()
    return accentTileSkin(accent = accent, onAccent = onAccentColor(accent))
}

@Composable
fun PresetSummary(
    preset: VocabularyPreset,
    languageTag: String,
    studySetState: PresetStudySetState,
    skin: TileSkin,
    onStudySetToggled: () -> Unit,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
        ) {
            Medallion(skin = skin, size = PresetMedallionSize) {
                MedallionIcon(presetIconFor(preset.icon), skin)
            }

            Column(modifier = Modifier.weight(1f)) {
                if (showTitle) {
                    Text(
                        text = preset.title.resolve(languageTag),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = skin.onTile,
                    )
                }
                Text(
                    text = preset.description.resolve(languageTag),
                    style = MaterialTheme.typography.bodySmall,
                    color = skin.muted(),
                    maxLines = DESCRIPTION_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            PresetInStudySetButton(state = studySetState, onClick = onStudySetToggled)
        }

        StatChip(
            icon = Icons.Default.Translate,
            text = "${preset.wordCount.grouped()} ${pluralStringResource(R.plurals.presets_word_count_label, preset.wordCount)}",
            skin = skin,
        )
    }
}

private const val DESCRIPTION_MAX_LINES = 2

@Composable
internal fun VocabularyPreset.accentColor(): Color = color.toAccentColor(MaterialTheme.colorScheme.primary)

internal fun String?.toAccentColor(fallback: Color): Color {
    val parsed = this?.removePrefix("#")?.toLongOrNull(radix = 16) ?: return fallback
    return Color(parsed or 0xFF000000L)
}

internal fun onAccentColor(accent: Color): Color = if (accent.luminance() > LIGHT_ACCENT_THRESHOLD) Color.Black else Color.White

private const val LIGHT_ACCENT_THRESHOLD = 0.34f

internal fun Int.grouped(): String = NumberFormat.getIntegerInstance().format(this)
