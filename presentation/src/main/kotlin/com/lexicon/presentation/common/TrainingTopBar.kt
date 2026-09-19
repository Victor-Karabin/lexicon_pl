package com.lexicon.presentation.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.LexiconTheme

val LocalCourseReset = staticCompositionLocalOf<(() -> Unit)?> { null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingTopBar(
    title: String,
    onClose: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val reset = LocalCourseReset.current

    TopAppBar(
        actions = {
            if (reset != null) {
                IconButton(onClick = reset) {
                    Icon(Icons.Default.RestartAlt, contentDescription = stringResource(R.string.vocabulary_course_reset))
                }
            }
            actions()
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = null)
            }
        },
    )
}

@LightDarkPreview
@Composable
private fun TrainingTopBarStatesPreview() {
    LexiconTheme {
        Surface {
            Column {
                TrainingTopBar(title = "Dictation", onClose = {})
                TrainingTopBar(title = "Pronunciation Check", onClose = {})

                TrainingTopBar(title = "A preset whose name is far longer than the bar", onClose = {})
            }
        }
    }
}
