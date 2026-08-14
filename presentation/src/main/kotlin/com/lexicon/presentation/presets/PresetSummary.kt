package com.lexicon.presentation.presets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lexicon.interactors.presets.PresetFavouriteState
import com.lexicon.interactors.presets.VocabularyPreset
import com.lexicon.interactors.presets.resolve
import com.lexicon.interactors.presets.wordCount
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.Dimens
import java.text.NumberFormat

private val IconBadgeSize = 44.dp

/**
 * How a preset introduces itself: its icon and word count, what it is, and whether
 * it is a favourite.
 *
 * The same block wherever a preset is shown — as a card in the Vocabulary tab, and
 * bare at the top of the preset's own screen. Only the surface around it differs, so
 * only that is left to the caller; the two had already drifted apart on icon size,
 * text styles and number formatting before this was shared.
 *
 * [showTitle] is off on the preset's own screen, where the top bar names it already.
 */
@Composable
fun PresetSummary(
    preset: VocabularyPreset,
    languageTag: String,
    favouriteState: PresetFavouriteState,
    onFavouriteToggled: () -> Unit,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(Dimens.spacingMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(IconBadgeSize).background(preset.accentColor(), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = presetIconFor(preset.icon),
                    contentDescription = null,
                    tint = Color.White,
                )
            }
            Text(
                text = preset.wordCount.grouped(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = Dimens.spacingSmall),
            )
            Text(
                text = pluralStringResource(R.plurals.presets_word_count_label, preset.wordCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            if (showTitle) {
                Text(
                    text = preset.title.resolve(languageTag),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = preset.description.resolve(languageTag),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = DESCRIPTION_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = Dimens.spacingTiny),
            )
        }

        PresetFavouriteButton(state = favouriteState, onClick = onFavouriteToggled)
    }
}

private const val DESCRIPTION_MAX_LINES = 2

@Composable
internal fun VocabularyPreset.accentColor(): Color = color.toAccentColor(MaterialTheme.colorScheme.primary)

/**
 * A `#RRGGBB` from the catalogue as a colour, opaque, falling back when it is
 * absent or malformed. Shared with the colour picker in the new-preset form, which
 * asks the same question of a swatch that has no preset behind it yet.
 */
internal fun String?.toAccentColor(fallback: Color): Color {
    val parsed = this?.removePrefix("#")?.toLongOrNull(radix = 16) ?: return fallback
    return Color(parsed or 0xFF000000L)
}

internal fun Int.grouped(): String = NumberFormat.getIntegerInstance().format(this)
