package com.lexicon.presentation.common

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InterceptPlatformTextInput

private val LocalTextInputActive = compositionLocalOf { false }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TrackTextInput(content: @Composable () -> Unit) {
    var sessions by remember { mutableIntStateOf(0) }
    InterceptPlatformTextInput(
        interceptor = { request, nextHandler ->
            sessions++
            try {
                nextHandler.startInputMethod(request)
            } finally {
                sessions--
            }
        },
    ) {
        CompositionLocalProvider(LocalTextInputActive provides (sessions > 0), content = content)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun isTyping(): Boolean = LocalTextInputActive.current && WindowInsets.isImeVisible
