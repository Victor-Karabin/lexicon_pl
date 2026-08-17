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

const val TILE_MUTED_ALPHA = 0.75f

private const val CHIP_ALPHA = 0.1f

private const val ACCENT_WASH_ALPHA = 0.22f

@Immutable
data class TileSkin(
    val sweep: List<Color>,
    val onTile: Color,
    val medallion: Color,
    val onMedallion: Color,
)

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

fun TileSkin.muted(): Color = onTile.copy(alpha = TILE_MUTED_ALPHA)

@Composable
fun GradientTile(
    skin: TileSkin,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = Dimens.spacingMedium,
    content: @Composable ColumnScope.() -> Unit,
) {
    val body: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(skin.sweep))
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(padding),
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

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
