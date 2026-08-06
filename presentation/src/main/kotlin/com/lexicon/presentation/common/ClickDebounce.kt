package com.lexicon.presentation.common

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

private const val DEFAULT_DEBOUNCE_MS = 500L

/** Wraps [onClick] so a rapid double-tap within [debounceTime] only fires once. */
@Composable
fun debounced(
    debounceTime: Long = DEFAULT_DEBOUNCE_MS,
    onClick: () -> Unit,
): () -> Unit {
    var lastClickTime by remember { mutableLongStateOf(0L) }

    return remember(onClick, debounceTime) {
        {
            val now = SystemClock.uptimeMillis()
            if (now - lastClickTime >= debounceTime) {
                lastClickTime = now
                onClick()
            }
        }
    }
}
