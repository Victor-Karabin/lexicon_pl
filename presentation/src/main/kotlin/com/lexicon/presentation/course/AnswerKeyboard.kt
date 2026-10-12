package com.lexicon.presentation.course

import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconShapes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val KeyHeight = 46.dp
private val KeyGap = 4.dp
private const val REPEAT_DELAY_MS = 400L
private const val REPEAT_INTERVAL_MS = 60L
private const val WIDE_KEY = 1.5f
private const val SPACE_KEY = 3.5f

private val polishRow = listOf("ą", "ć", "ę", "ł", "ń", "ó", "ś", "ź", "ż")
private val letterRows = listOf(
    listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
    listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
    listOf("z", "x", "c", "v", "b", "n", "m"),
)
private val digitRows = listOf(
    listOf("1", "2", "3", "-"),
    listOf("4", "5", "6", ":"),
    listOf("7", "8", "9", "≠"),
    listOf("/", "0", "+"),
)
private val sentenceEnd = Regex("""(^|[.!?]\s+)$""")

@Composable
internal fun AnswerKeyboard(
    session: AnswerKeyboardSession,
    onClosed: () -> Unit,
) {
    var shifted by remember(session) { mutableStateOf(false) }
    var capsLock by remember(session) { mutableStateOf(false) }
    var digits by remember(session) { mutableStateOf(session.expectsDigits) }

    fun autoCapitalize() {
        if (capsLock) return
        val before = session.connection.getTextBeforeCursor(2, 0)?.toString().orEmpty()
        shifted = session.capitalizesAll || (session.capitalizesSentences && sentenceEnd.containsMatchIn(before))
    }

    LaunchedEffect(session) { autoCapitalize() }

    fun type(text: String) {
        session.connection.commitText(if (shifted || capsLock) text.uppercase() else text, 1)
        autoCapitalize()
    }

    fun delete() {
        val selected = session.connection.getSelectedText(0)
        if (selected.isNullOrEmpty()) session.connection.deleteSurroundingText(1, 0) else session.connection.commitText("", 1)
        autoCapitalize()
    }

    fun act() {
        when {
            session.insertsNewLine -> type("\n")
            session.action == EditorInfo.IME_ACTION_NEXT -> session.connection.performEditorAction(EditorInfo.IME_ACTION_NEXT)
            else -> onClosed()
        }
    }

    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.navigationBarsPadding().padding(horizontal = KeyGap, vertical = Dimens.spacingSmall),
            verticalArrangement = Arrangement.spacedBy(KeyGap * 2),
        ) {
            if (digits) {
                digitRows.forEachIndexed { index, row ->
                    KeyRow {
                        row.forEach { key -> LetterKey(key, false) { session.connection.commitText(key, 1) } }
                        if (index == digitRows.lastIndex) DeleteKey(weight = 1f, onDelete = ::delete)
                    }
                }
            } else {
                LetterRows(
                    upper = shifted || capsLock,
                    shiftHighlighted = shifted || capsLock,
                    onLetter = ::type,
                    onShift = {
                        when {
                            capsLock -> capsLock = false.also { shifted = false }
                            shifted -> capsLock = true
                            else -> shifted = true
                        }
                    },
                    onDelete = ::delete,
                )
            }
            KeyRow {
                TextKey(
                    label = stringResource(if (digits) R.string.answer_keyboard_letters else R.string.answer_keyboard_digits),
                    weight = WIDE_KEY,
                    onClick = { digits = !digits },
                )
                LetterKey(",", false) { type(",") }
                TextKey(stringResource(R.string.answer_keyboard_space), weight = SPACE_KEY) { type(" ") }
                LetterKey(".", false) { type(".") }
                LetterKey("?", false) { type("?") }
                ActionKey(session, ::act)
            }
        }
    }
}

@Composable
private fun LetterRows(
    upper: Boolean,
    shiftHighlighted: Boolean,
    onLetter: (String) -> Unit,
    onShift: () -> Unit,
    onDelete: () -> Unit,
) {
    KeyRow { polishRow.forEach { letter -> LetterKey(letter, upper) { onLetter(letter) } } }
    KeyRow { letterRows[0].forEach { letter -> LetterKey(letter, upper) { onLetter(letter) } } }
    KeyRow(inset = true) { letterRows[1].forEach { letter -> LetterKey(letter, upper) { onLetter(letter) } } }
    KeyRow {
        IconKey(
            icon = Icons.Default.ArrowUpward,
            description = stringResource(R.string.answer_keyboard_shift),
            weight = WIDE_KEY,
            highlighted = shiftHighlighted,
            onClick = onShift,
        )
        letterRows[2].forEach { letter -> LetterKey(letter, upper) { onLetter(letter) } }
        DeleteKey(weight = WIDE_KEY, onDelete = onDelete)
    }
}

@Composable
private fun RowScope.DeleteKey(
    weight: Float,
    onDelete: () -> Unit,
) {
    RepeatingIconKey(
        icon = Icons.AutoMirrored.Filled.Backspace,
        description = stringResource(R.string.answer_keyboard_delete),
        weight = weight,
        onPress = onDelete,
    )
}

@Composable
private fun KeyRow(
    inset: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = if (inset) Dimens.spacingMedium else 0.dp),
        horizontalArrangement = Arrangement.spacedBy(KeyGap),
        content = content,
    )
}

@Composable
private fun RowScope.LetterKey(
    letter: String,
    upper: Boolean,
    onClick: () -> Unit,
) {
    TextKey(if (upper) letter.uppercase() else letter, weight = 1f, onClick = onClick)
}

@Composable
private fun RowScope.TextKey(
    label: String,
    weight: Float,
    onClick: () -> Unit,
) {
    KeyBox(weight = weight, description = label, highlighted = false, onPress = onClick) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun RowScope.IconKey(
    icon: ImageVector,
    description: String,
    weight: Float,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    KeyBox(weight = weight, description = description, highlighted = highlighted, onPress = onClick) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun RowScope.RepeatingIconKey(
    icon: ImageVector,
    description: String,
    weight: Float,
    onPress: () -> Unit,
) {
    KeyBox(weight = weight, description = description, highlighted = false, onPress = onPress, repeats = true) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun RowScope.ActionKey(
    session: AnswerKeyboardSession,
    onClick: () -> Unit,
) {
    val (icon, description) = when {
        session.insertsNewLine -> Icons.AutoMirrored.Filled.KeyboardReturn to stringResource(R.string.answer_keyboard_new_line)
        session.action == EditorInfo.IME_ACTION_NEXT ->
            Icons.AutoMirrored.Filled.KeyboardArrowRight to stringResource(R.string.answer_keyboard_next)
        else -> Icons.Default.Check to stringResource(R.string.answer_keyboard_done)
    }
    KeyBox(weight = WIDE_KEY, description = description, highlighted = true, onPress = onClick) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
private fun RowScope.KeyBox(
    weight: Float,
    description: String,
    highlighted: Boolean,
    onPress: () -> Unit,
    repeats: Boolean = false,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }
    val background = when {
        highlighted -> MaterialTheme.colorScheme.primary
        pressed -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    Box(
        modifier = Modifier
            .weight(weight)
            .height(KeyHeight)
            .background(background, LexiconShapes.small)
            .semantics {
                contentDescription = description
                role = Role.Button
            }
            .pointerInput(onPress, repeats) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onPress()
                        val repeating = if (repeats) {
                            scope.launch {
                                delay(REPEAT_DELAY_MS)
                                while (true) {
                                    onPress()
                                    delay(REPEAT_INTERVAL_MS)
                                }
                            }
                        } else {
                            null
                        }
                        tryAwaitRelease()
                        repeating?.cancel()
                        pressed = false
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
