package com.lexicon.presentation.course

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

class LetterTarget {
    var insert: ((String) -> Unit)? by mutableStateOf(null)
}

val LocalLetterTarget = compositionLocalOf<LetterTarget?> { null }

@Composable
fun rememberEditableText(value: String): MutableState<TextFieldValue> {
    val state = remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    LaunchedEffect(value) {
        if (state.value.text != value) state.value = TextFieldValue(value, TextRange(value.length))
    }
    return state
}

@Composable
fun rememberLetterReceiver(
    state: MutableState<TextFieldValue>,
    onValueChanged: (String) -> Unit,
): Modifier {
    val target = LocalLetterTarget.current ?: return Modifier
    val latest by rememberUpdatedState(onValueChanged)
    return Modifier.onFocusChanged { focus ->
        if (!focus.isFocused) return@onFocusChanged
        target.insert = { letter ->
            val current = state.value
            val text = current.text.replaceRange(current.selection.min, current.selection.max, letter)
            state.value = TextFieldValue(text, TextRange(current.selection.min + letter.length))
            latest(text)
        }
    }
}
