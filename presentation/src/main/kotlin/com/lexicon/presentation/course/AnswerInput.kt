package com.lexicon.presentation.course

import android.os.LocaleList
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import java.util.Locale

private val polish = LocaleList(Locale.forLanguageTag("pl-PL"))

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AnswerInputs(content: @Composable () -> Unit) {
    InterceptPlatformTextInput(
        interceptor = { request, nextHandler ->
            nextHandler.startInputMethod(
                object : PlatformTextInputMethodRequest {
                    override fun createInputConnection(outAttributes: EditorInfo): InputConnection {
                        val connection = request.createInputConnection(outAttributes)
                        outAttributes.withoutSuggestions()
                        return connection
                    }
                },
            )
        },
        content = content,
    )
}

private fun EditorInfo.withoutSuggestions() {
    hintLocales = polish
    imeOptions = imeOptions or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
    if (inputType and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT) return
    inputType = InputType.TYPE_CLASS_TEXT or
        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or
        InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
        (inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE)
}
