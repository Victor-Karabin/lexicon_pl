package com.lexicon.presentation.passage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconSuccess

private val CharacterWidth = 11.dp
private val MinWidth = 56.dp
private val MaxWidth = 200.dp

@Composable
fun PassageGap(
    value: String,
    expected: String,
    isCorrect: Boolean?,
    isChecked: Boolean,
    readOnly: Boolean,
    onValueChanged: (String) -> Unit,
    onCleared: () -> Unit,
) {
    val underline = when (isCorrect) {
        true -> LexiconSuccess
        false -> LexiconError
        null -> MaterialTheme.colorScheme.outline
    }
    val width = (CharacterWidth * expected.length).coerceIn(MinWidth, MaxWidth)

    if (readOnly) {
        Column(
            modifier = Modifier
                .width(width)
                .clickable(enabled = !isChecked && value.isNotBlank(), onClick = onCleared),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.spacingSmall),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = value, style = MaterialTheme.typography.bodyLarge)
            }
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(underline))
        }
        return
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChanged,
        enabled = !isChecked,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { field ->
            Column(modifier = Modifier.width(width)) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.spacingSmall),
                    contentAlignment = Alignment.Center,
                    content = { field() },
                )
                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(underline))
            }
        },
    )
}
