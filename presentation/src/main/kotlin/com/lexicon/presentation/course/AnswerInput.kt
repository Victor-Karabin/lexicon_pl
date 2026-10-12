package com.lexicon.presentation.course

import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalFocusManager
import com.lexicon.presentation.common.LocalAnswerKeyboardShown
import kotlinx.coroutines.awaitCancellation

class AnswerKeyboardSession(
    val connection: InputConnection,
    val editorInfo: EditorInfo,
) {
    val insertsNewLine: Boolean
        get() = editorInfo.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0 &&
            action in setOf(EditorInfo.IME_ACTION_NONE, EditorInfo.IME_ACTION_UNSPECIFIED)

    val capitalizesSentences: Boolean get() = editorInfo.inputType and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES != 0

    val capitalizesAll: Boolean get() = editorInfo.inputType and InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS != 0

    val expectsDigits: Boolean get() = editorInfo.inputType and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT

    val action: Int get() = editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AnswerInputs(content: @Composable () -> Unit) {
    var session by remember { mutableStateOf<AnswerKeyboardSession?>(null) }
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = session != null) { focusManager.clearFocus() }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            CompositionLocalProvider(LocalAnswerKeyboardShown provides (session != null)) {
                InterceptPlatformTextInput(
                    interceptor = { request, _ ->
                        val info = EditorInfo()
                        val started = AnswerKeyboardSession(request.createInputConnection(info), info)
                        session = started
                        try {
                            awaitCancellation()
                        } finally {
                            if (session === started) session = null
                        }
                    },
                    content = content,
                )
            }
        }
        session?.let { active -> AnswerKeyboard(session = active, onClosed = { focusManager.clearFocus() }) }
    }
}
