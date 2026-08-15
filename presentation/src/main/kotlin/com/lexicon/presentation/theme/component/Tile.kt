package com.lexicon.presentation.theme.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconShapes

val MedallionSize = 52.dp
private val StatIconSize = 14.dp

/** Secondary text on a tile, whichever coat the tile is wearing. */
const val TILE_MUTED_ALPHA = 0.75f

/** Just enough tint for a chip to read against the tile behind it. */
private const val CHIP_ALPHA = 0.1f

/** How much of an accent colour a tile takes, before it stops being a background. */
private const val ACCENT_WASH_ALPHA = 0.22f

/**
 * The colours one tile wears.
 *
 * A tile is the app's unit of "something to open": a program, a training, a preset, a
 * course. They differ in what they hold and not in how they are read, so the palette
 * is one decision rather than four, and a screen picks a coat instead of assembling
 * colours by hand.
 */
@Immutable
data class TileSkin(
    val sweep: List<Color>,
    val onTile: Color,
    val medallion: Color,
    val onMedallion: Color,
)

/**
 * The standard two coats: loud for whatever is under way, quiet for the rest.
 *
 * Highlighting is what makes a list scannable — one tile answers "where was I?" before
 * any of the text is read.
 */
@Composable
fun tileSkin(highlighted: Boolean = false): TileSkin {
    val scheme = MaterialTheme.colorScheme
    return if (highlighted) {
        TileSkin(
            sweep = listOf(scheme.primaryContainer, scheme.tertiaryContainer),
            onTile = scheme.onPrimaryContainer,
            medallion = scheme.primary,
            onMedallion = scheme.onPrimary,
        )
    } else {
        TileSkin(
            sweep = listOf(scheme.surfaceContainerHighest, scheme.secondaryContainer),
            onTile = scheme.onSurface,
            medallion = scheme.primaryContainer,
            onMedallion = scheme.onPrimaryContainer,
        )
    }
}

/**
 * A coat built around a colour the content owns — a preset's own accent, say.
 *
 * Washed out rather than used at full strength, because the accent has to sit behind
 * text that is not chosen with it in mind.
 */
@Composable
fun accentTileSkin(
    accent: Color,
    onAccent: Color,
): TileSkin =
    TileSkin(
        sweep = listOf(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            accent.copy(alpha = ACCENT_WASH_ALPHA),
        ),
        onTile = MaterialTheme.colorScheme.onSurface,
        medallion = accent,
        onMedallion = onAccent,
    )

/** Secondary text and icons on a tile: present, but not competing with the title. */
fun TileSkin.muted(): Color = onTile.copy(alpha = TILE_MUTED_ALPHA)

/**
 * The card everything sits in: rounded, swept with colour, and tappable when there is
 * somewhere to go.
 */
@Composable
fun GradientTile(
    skin: TileSkin,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val body: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(skin.sweep))
                .padding(Dimens.spacingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
            content = content,
        )
    }

    if (onClick == null) {
        Surface(
            shape = LexiconShapes.medium,
            color = Color.Transparent,
            modifier = modifier.fillMaxWidth(),
            content = body,
        )
    } else {
        Surface(
            onClick = onClick,
            shape = LexiconShapes.medium,
            color = Color.Transparent,
            modifier = modifier.fillMaxWidth(),
            content = body,
        )
    }
}

/** The round badge a tile leads with: a level, a count, or the thing's own icon. */
@Composable
fun Medallion(
    skin: TileSkin,
    modifier: Modifier = Modifier,
    size: Dp = MedallionSize,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.size(size).background(skin.medallion, CircleShape),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

@Composable
fun MedallionText(
    text: String,
    skin: TileSkin,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = skin.onMedallion,
    )
}

@Composable
fun MedallionIcon(
    icon: ImageVector,
    skin: TileSkin,
) {
    Icon(imageVector = icon, contentDescription = null, tint = skin.onMedallion)
}

/**
 * One fact about a tile, small enough to sit beside two others.
 *
 * Chips rather than a sentence, because the numbers are what gets compared between
 * one tile and the next.
 */
@Composable
fun StatChip(
    icon: ImageVector,
    text: String,
    skin: TileSkin,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = LexiconShapes.small,
        color = skin.onTile.copy(alpha = CHIP_ALPHA),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.spacingSmall, vertical = Dimens.spacingTiny),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingTiny),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = skin.onTile,
                modifier = Modifier.size(StatIconSize),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = skin.onTile,
                // A chip is one short fact and stays on one line. Given less width than
                // it wants it would otherwise wrap, and a chip narrower than its longest
                // word wraps *per letter* — a column of single characters where a phrase
                // should be. Truncating says the same thing and keeps the tile readable.
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The strip of chips along the bottom of a tile.
 *
 * Flowed rather than in a row: three chips fit across a wide phone and do not fit a
 * narrow one at a large text size, and the row has no way to say so — it shares out
 * what is left and each chip shrinks until its text is a column of letters. Flowing
 * moves the chip that will not fit onto a line of its own instead.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TileChips(
    modifier: Modifier = Modifier,
    content: @Composable FlowRowScope.() -> Unit,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        content = content,
    )
}
